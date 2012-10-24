(ns session.client.loop
  (:require
   [session.client.mvc :as mvc]
   [session.client.editor :as editor]
   [cljs.reader :as reader]
   [session.client.loop-creator :as loop-creator]

   )
  (:use-macros [cljs-jquery.macros :only [$]])
  (:require-macros [fetch.macros :as pm])
  )


(deftype Loop [model]
     IPrintable
  (-pr-seq [a opts]
    (concat  ["#session/loop "] (-pr-seq (assoc model :input @(:input model) :output @(:output model)) opts) ""))

   ILookup
  (-lookup [o k] (model k))
  (-lookup [o k not-found] (model k not-found))
    session.client.mvc/IMVC
    (view [model]

    (let [id (:id model)]
    ($
     [:div.loop-container
      ;;[:div.span6.row ]
      [:div.row.input {:id id}
       [:div.span6
        [:i.icon-chevron-right {:style "float:left"} ""]
        [:textarea.span5
         {:id (str "area" id)
          ;;:style "margin-left:0px;position:relative;height:18px"
          }
         @(:input model)]
        [:a.close.loop-deleter {:href "#" :id (str "delete" id) :style "margin-left:10px;float:none"} "x"]
        ]
       ]
      [:div.row {:id (str "out" id)}
       [:div.span6
        [:i.icon-chevron-left {:style "float:left"} ""]
        [:div.span5.loopout {:style "margin-left:0px;position:relative"}
         (session.client.mvc/view @(:output model))]
        ]
       ]
      (mvc/render (loop-creator/LoopCreator. false)
        )
      ]
     (data "model" model))))
  (control [model viewobject]
    (let [model ($ viewobject (data "model")) id (:id model) editor (atom [])]
      ($ viewobject (on "click" ".loop-creator" #(do ($ viewobject (trigger "insert-new-loop")) )))
    ;;($ viewobject (on "click" "a.close"))
      ($ viewobject (on "post-render" #(do
                                         (reset! editor (editor/create-editor (str "area" id)))
                                         ;;(editor/fit-to-length (str "area" id) @editor)
                                         )))
      ;;(reset! editor (editor/create-editor ($ viewobject (get 0))))
      ;;(editor/fit-to-length (str "area" id) @editor)
      ($ viewobject (on "click" ".loop-deleter" #($ viewobject (trigger "delete-loop"))))
      ($ viewobject (on "evaluate-input"
                      #(do
                         ;;(js/alert "evaluate-input")
                         (reset!
                          (:input model)
                          (.  @editor (getValue)))
                         ($ viewobject (trigger "evaluate-loop")))))
      (add-watch (:output model) :update-output
                 (fn [key atom old new]
                   (if (= cljs.core.Atom (type new))
                     (do
                       (js/alert "add atom to watch")
                       (add-watch new :update-from-atom
                                  (fn [key2 atom2 old2 new2]

                                    (js/alert "atom update")
                                    ($ viewobject
                                       (find ".loopout") (html "")
                                       (append ($ [:div (mvc/view new2)])))))
                       ($ viewobject (find ".loopout") (html "") (append ($ [:div (mvc/view @new)]))))
                     ($ viewobject (find ".loopout") (html "") (append ($ [:div (mvc/view new)])))))))))

(reader/register-tag-parser! "loop" (fn [x] (Loop. (assoc x :input (atom (:input x)) :output (atom (:output x)))) ))
