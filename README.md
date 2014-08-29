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
  - [ ] Read textures (EngineTextures.dat, what does the EngineTextures.wad file contain?)
    - [x] Names & entries
    - [ ] Texture compression
  - [ ] Read sounds (*
  - [ ]Read cutscenes (*
  - ?
- [ ] Conversion of formats
  - [ ] Meshes to JMonkeyEngine j3o
    - [x] Basic mesh conversion
    - [ ] LOD
    - [ ] Animations (**
    - [ ] Materials
  - [ ] Maps to our open map format (XML, xstream?)
  - [ ] Sound player for DK format (or to OGG, loss of quality but easy to replace with own sounds) (*
  - [ ] Video playes for DK format (-||-) (*
  - ?
- [ ] Load maps to engine
- [ ] Basic world interaction
- [ ] Basic AI
- [ ] Main menu
- [ ] In game menu
- [ ] Save & load
- ?
  
(* low priority
(** vertex animations, we need our own animation control, JME doesn't support out of the box

Resources
=========

http://keeperklan.com/threads/4623-Reversal-of-DKII-Binary-File-Formats
http://keeperklan.com/threads/220-DK2-texture-format
https://code.google.com/p/jadex-agentkeeper/
https://github.com/werkt/kwd
