(ns dream-shrine.maps)

(def minimap-overworld-graphics-offset 0x81697) ;; TODO: what bank is this? what label/symbol is this?
(def minimap-overworld-graphics-size 0x100)

(def minimap-overworld-palette-offset 0x81797) ;; TODO: what bank is this? what label/symbol is this?
(def minimap-overworld-palette-size 0x100)

(def minimap-overworld-palette-colors-offset 0x8786E)
(def minimap-overworld-palette-colors-size 1) ;; TODO: what is the correct value? see LALE's GetColor function?

(def minimap-overworld-tiles-offset 0xB3800) ;; TODO: what bank is this? what label/symbol is this?
(def minimap-overworld-tiles-size 0x800)

(comment
  (defn draw-minimap-overworld-tiles
    "ported from LALE.MinimapDrawer/drawOverworldTiles which goes
  hand-in-hand with LALE.MinimapDrawer/loadMinimapOverworld,"
    [rom]
    (let [minimap-overworld-graphics (read-bytes rom
                                                 minimap-overworld-graphics-offset
                                                 minimap-overworld-graphics-size)
          minimap-overworld-palette (read-bytes rom
                                                minimap-overworld-palette-offset
                                                minimap-overworld-palette-size)

          minimap-overworld-palette-colors (read-bytes rom
                                                       minimap-overworld-palette-colors-offset
                                                       minimap-overworld-palette-colors-size)
          minimap-overworld-tiles (read-bytes rom
                                              minimap-overworld-tiles-offset
                                              minimap-overworld-tiles-size)])))
        
  
