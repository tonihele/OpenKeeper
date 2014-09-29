OpenDungeonKeeper
=================

Dungeon Keeper II clone

Goal is to fully implement the game (version 1.7 with 3 bonus packs) as open source version, with minimal or no changes at all, using the original game assets. So it will require the original game to play / develop. Futher development could have fan made graphics (to at least enable standalone version) and features.

Implementation is done in JAVA using JMonkeyEngine (http://jmonkeyengine.org/).

TODO
====

- [ ] Read the original assets
  - [x] Extract WAD files
    - [x] Support compression
  - [x] Read meshes (meshes.wad -> kmf)
  - [ ] Read maps (kwd, kld)
  - [ ] Read textures (EngineTextures.dat, what does the EngineTextures.wad file contain? - PNGs..)
    - [x] Names & entries
    - [ ] Texture compression
  - [x] Read sounds
  - [ ] Read cutscenes (*
  - [ ] Read paths
    - [x] Read paths (paths.wad -> kcs)
    - [ ] Understand paths
  - [x] Read cursors
  - ?
- [ ] Conversion of formats
  - [ ] Meshes to JMonkeyEngine j3o
    - [x] Basic mesh conversion
    - [x] LOD
    - [x] Animations (**
      - [ ] Integrate animations to the model itself
    - [x] Materials
      - [ ] All materials should exist only once, as JME material file
  - [ ] Maps to our open map format (XML, xstream?)
  - [ ] Sounds (MP2 is not supported by JME, either write support or convert to WAV/OGG, Xuggler?) (*
  - [ ] Video playes for DK format (*
  - [ ] Paths to JME MotionPaths?
  - [x] Cursors
  - ?
- [ ] Load maps to engine
- [ ] Basic world interaction
- [ ] Basic AI
- [ ] Main menu
- [ ] In game menu
- [ ] Save & load
- ?
  
`(*` low priority

`(**` vertex animations, we need our own animation control, JME doesn't support out of the box

Resources
=========

http://keeperklan.com/threads/4623-Reversal-of-DKII-Binary-File-Formats

http://keeperklan.com/threads/220-DK2-texture-format

https://code.google.com/p/jadex-agentkeeper/

https://github.com/werkt/kwd

[Sound & Video formats](http://wiki.multimedia.cx/index.php?title=Electronic_Arts_Formats)

http://simonschreibt.de/gat/dungeon-keeper-2-walls/
