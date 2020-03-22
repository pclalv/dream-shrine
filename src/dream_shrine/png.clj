(ns dream-shrine.png
  "For extracting image data from the ROM."
  (:require [clojure.java.io :as io]))

;; try to port this code to clj.
;; https://github.com/mattcurrie/mgbdis/blob/master/mgbdis.py#L687

;; def process_image_in_range(self, rom, start_address, end_address, arguments = None):
;;     if not self.first_pass and debug:
;;         print('Outputting image in range: {} - {}'.format(hex_word(start_address), hex_word(end_address)))

;;     if self.first_pass:
;;         return

;;     mem_address = rom_address_to_mem_address(start_address)
;;     labels = self.get_labels_for_non_code_address(mem_address)
;;     if len(labels):
;;         self.append_labels_to_output(labels)
;;         basename = labels[0].rstrip(':')
;;     else:
;;         basename = self.format_image_label(mem_address)

;;     full_filename = rom.write_image(basename, arguments, '2bpp', rom.data[start_address:end_address])
;;     self.append_output(self.format_instruction('INCBIN', ['\"' + full_filename + '\"'])))


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
                                                   1))]))]
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

(defn write-image
  "ported from mgbdis"
  [rom basename {:keys [width
                        palette]
                 :or {width 128
                      palette 0xe4}}]
  ;; to start, we're gonna try treat the whole ROM as one big block of
  ;; image data. then we'll whittle it down.
  (let [bytes-per-tile-row 2 ;; 8 pixels at 2 bits per pixel
        bytes-per-tile     bytes-per-tile-row * 8 ;; 8 rows per tile

        filename "test.png"
        ;; image-output-path (io/file output-dir image-output-dir filename)
        image-output-path (join-path output-dir image-output-dir filename)
        _ (io/make-parents image-output-path)

        data rom ;; TODO: handle a subset of the ROM. for now we're
        ;; just gonna imagine that the whole ROM is graphics
        ;; data.

        num-tiles (/ (count data) bytes-per-tile)
        tiles-per-row (/ width 8)

        tile-rows (/ num-tiles tiles-per-row)

        height (* tile-rows 8)

        pixel-data (convert-to-pixel-data data width height)

        [[palette-r palette-g palette-b]] (-> palette
                                              convert-palette-to-rgb
                                              transpose)

        ;; TODO: have convert-palette-to-rgb return IndexColorModel?
        bpp 2
        palette-size (bit-shift-left bpp 1)
        icm (IndexColorModel. bpp
                              palette-size
                              (byte-array palette-r)
                              (byte-array palette-g)
                              (byte-array palette-b))
        bi (BufferedImage. width height BufferedImage.TYPE_INT_ARGB icm)];
    
    ;; now to write a PNG somehow
    ;; https://stackoverflow.com/questions/6973290/generate-and-save-a-png-image-in-clojure
    (with-open [w (io/writer image-output-path)]
      (.write w))))
