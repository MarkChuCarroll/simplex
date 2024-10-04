
(defvar simplex-keywords nil "simplex keywords")
(setq simplex-keywords '("var" "fun" "meth" "tup" "produce" "do" "end"))

(defvar simplex-exprwords nil "simplex expression words")
(setq simplex-exprwords '("for" "in" "let" "with" "update" "if" "then" "elif" "else"))

(defvar simplex-types nil "simplex builtin type names")
(setq simplex-types '("Boolean" "Int" "Float" "String" "CSG" "ThreeDPoint"
                      "TwoDPoint" "Array" "Function" "Polygon"))



(defvar simplex-fontlock nil "font-lock defaults")
(setq simplex-fontlock
      (let (simplex-keywords-regex simplex-exprwords-regex simplex-types-regex)
        (setq simplex-keywords-regex (regexp-opt simplex-keywords 'words))
        (setq simplex-exprwords-regex (regexp-opt simplex-exprwords 'words))
        (setq simplex-type-regex (regexp-opt simplex-types 'words ))

        (list (cons simplex-exprwords-regex 'font-lock-constant-face)
              (cons simplex-keywords-regex 'font-lock-keyword-face)
              (cons simplex-type-regex 'font-lock-type-face))))

(define-derived-mode simplex-mode fundamental-mode "simplex"
   "major mode for editing simplex language code."
   (setq font-lock-defaults '(simplex-fontlock)))

(add-to-list 'auto-mode-alist '("\\.s3d\\'" . simplex-mode))
(provide 'simplex-mode)
