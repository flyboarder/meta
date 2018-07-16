(ns meta.boot.templates
  (:require [boot.core :as boot]
            [boot.util :as util]
            [meta.boot.impl :as impl]
            [meta.boot.util :as mutil]
            [clojure.java.io :as io]
            [stencil.core :as stencil]))

(defn- render-template
  "Render and output a file using stencil/mustache.

    `dir`  - The output directory
    `file` - The output file name with extension.
    `tmpl` - The template string used for rendering.
    `data` - The data to be passed to the template string.

  "
  [dir file tmpl data]
  (mutil/spit-file dir file (stencil/render-string tmpl data)))

(defn- slurp-file
  "Slurp a boot tmp-file. (will not read/parse)"
  [file]
  (slurp (boot/tmp-file file)))

(defn- read-file
  "Read a boot tmp-file. (will read/parse)"
  [file]
  (read-string (slurp-file file)))

(defn- render-file
  "Render and output a file from template and edn files.

    `dir`  - The output directory
    `file` - The output file name with extension.
    `tmpl` - The template file used for rendering.
    `edn`  - The edn file to be passed to the template.

  "
  [dir file tmpl edn]
  (render-template dir file (slurp-file tmpl) (read-file edn)))

(defn- warn-missing
  [file] (util/warn "• %s...: missing!\n" file))

(boot/deftask project-templates
  "Generate project files from templates."
  [n namespaces VAL [[str str str]] "Project namespaces to generate."]
  (boot/with-pre-wrap fs
    (let [tmp      (boot/tmp-dir!)
          in-files (boot/input-files fs)]
      (util/info "Compiling project template files...\n")
      (doseq [path paths]
        (let [files     (->> in-files (boot/by-re [(re-pattern (str "^" path))]))
              cljs-path (format "%s.cljs"     path)
              edn-path  (format "%s.edn"      path)
              tmpl-path (format "%s.mustache" path)]
          ;; search for template file
          (if-let [tmpl-file (->> files (boot/by-ext [".mustache"]) first)]
            ;; search for edn file
            (if-let [edn-file (->> files (boot/by-ext [".edn"]) first)]
              ;; generate cljs from edn and template files
              (render-file tmp cljs-path tmpl-file edn-file)
              ;; warn missing edn file
              (warn-missing edn-path))
            ;; warn missing template file
            (warn-missing tmpl-path))))
      (-> fs (boot/add-resource tmp) boot/commit!))))

(boot/deftask generate-cljs
  "Generate ClojureScript files from templates."
  [n namespaces VAL [sym] "CLJS namespaces to generate."]
  (let [gen-ns (:namespaces *opts*)
        paths  (map mutil/ns->path gen-ns)
        exts   [".cljs" ".edn" ".mustache"]]))
