(ns dream-shrine.png
  "For extracting image data from the ROM."
  (:require [clojure.java.io :as io])
  (:import [java.awt Point]
           [java.awt.image BufferedImage DataBufferByte IndexColorModel Raster]
           [java.io ByteArrayInputStream ByteArrayOutputStream]
           [javax.imageio ImageIO]))

;; try to port this code to clj.
;; https://github.com/mattcurrie/mgbdis/blob/master/mgbdis.py#L687

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [f]
  (clojure.core/with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream f) out)
    (->> out .toByteArray
         (mapv #(java.lang.Byte/toUnsignedInt %)))))

(def rom (-> "ladx-1.0.gbc"
             clojure.java.io/resource
             slurp-bytes))

(def output-dir ".")
(def image-output-dir "images")

(defn join-path [p & ps]
  (str (.normalize (java.nio.file.Paths/get p (into-array String ps)))))

(defn coordinate-to-tile-offset
  "ported from mgbdis"
  [x y width]
  (let [bytes-per-tile-row 2                        ;; 8 pixels at 2 bits per pixel
        bytes-per-tile     (* bytes-per-tile-row 8) ;; 8 rows per tile
        tiles-per-row (/ width 8)
        tile-y        (/ y 8)
        tile-x        (/ x 8)
        row-of-tile   (bit-and y 7)]
    (+ (* tile-y tiles-per-row bytes-per-tile)
       (* tile-x bytes-per-tile)
       (* row-of-tile bytes-per-tile-row))))

(defn convert-to-pixel-data
  "ported from mgbdis"
  [data width height]  
  (for [y (range height)]
    (for [x (range width)]
      (let [offset (coordinate-to-tile-offset x y width)
            color (if (>= offset (count data))
                    0
                    ;; extract the color from the two bytes of tile data at the offset
                    (let [shift (- 7 (bit-and x 7))
                          mask (bit-shift-left 1 shift)
                          color (+ (bit-shift-right (bit-and (nth data offset)
                                                             mask)
                                                    shift)
                                   (bit-shift-left (bit-shift-right (bit-and (nth data (inc offset))
                                                                             mask)
                                                                    shift)
                                                   1))]
                      color))]
        color))))

(defn transpose [m]
  (apply mapv vector m))

(defn convert-palette-to-rgb
  "ported from mgbdis"
  [palette]
  (let [col0 (- 255
                (bit-shift-left (bit-and palette 0x03)
                                6))
        col1 (- 255
                (bit-shift-left (bit-shift-right (bit-and palette 0x0C)
                                                 2)
                                6))
        col2 (- 255
                (bit-shift-left (bit-shift-right (bit-and palette 0x30)
                                                 4)
                                6))
        col3 (- 255
                (bit-shift-left (bit-shift-right (bit-and palette 0xC0)
                                                 6)
                                6))]
    ;; r    g    b    ?
    [[col0 col0 col0]
     [col1 col1 col1]
     [col2 col2 col2]
     [col3 col3 col3]]))

(defn rgb->icm [[palette-r palette-g palette-b]]
  (let [bpp 2
        palette-size (bit-shift-left bpp 1)]
    (IndexColorModel. bpp
                      palette-size
                      (byte-array palette-r)
                      (byte-array palette-g)
                      (byte-array palette-b))))

;; seems like we can effectively decode bytes as
;; PNGs... unfortunately, i have no idea where in the ROM any of the
;; text is. i think that the next thing to do will be to do some cljfx
;; learning, so that i can display the PNG that i'm generating and
;; play with it interactively. for example, can we play with any of
;; these parameters in order to generate a more coherent image?
(defn generate-image
  "ported from mgbdis"
  [rom {:keys [offset size]} {:keys [width palette]
                              :or {width 128
                                   palette 0xe4}}]
  (let [bytes-per-tile-row 2                         ;; 8 pixels at 2 bits per pixel
        bytes-per-tile     (* bytes-per-tile-row  8) ;; 8 rows per tile

        data (cond (and offset size)
                   #_=> (->> rom
                             (drop offset)
                             (take size))
                   (or (and (not offset) size)
                       (and offset (not size)))
                   #_=> (throw (Exception. "You must provide both offset and size"))

                   :else
                   #_=> rom)

        num-tiles (/ (count data) bytes-per-tile)
        tiles-per-row (/ width 8)

        tile-rows (/ num-tiles tiles-per-row)

        height (* tile-rows 8)

        pixel-data (convert-to-pixel-data data width height)

        icm (-> palette
                convert-palette-to-rgb
                transpose
                rgb->icm)

        bi (BufferedImage. width height BufferedImage/TYPE_BYTE_INDEXED icm)
        raster (Raster/createRaster (-> bi .getSampleModel)
                                    (DataBufferByte. (->> pixel-data
                                                          flatten
                                                          byte-array)
                                                     (count pixel-data))
                                    (Point.))
        _ (-> bi (.setData raster))]
    bi))

(defn buffered-image->input-stream
  "lifted from https://stackoverflow.com/questions/4251383/how-to-convert-bufferedimage-to-inputstream/21569243#21569243"
  [bi]
  (with-open [os (new ByteArrayOutputStream)]
    (ImageIO/write bi "png" os)
    (ByteArrayInputStream. (.toByteArray os))))

(defn write-image [bi basename]
  (let [filename (str basename ".png")
        image-output-path (join-path output-dir image-output-dir filename)

        _ (io/make-parents image-output-path)

        file (io/file image-output-path)]
    (ImageIO/write bi "png" file)))
