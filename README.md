# Friya's Cinematics
A client-side mod for Wurm Unlimited that assist you in making better looking video content.

This is a few years after that fact and I recall there being a site with documentation accompanying this mod,
~~sadly it's nowhere to be found~~ it can now be found [here](https://github.com/romland/wurm-cinematics-site).

Examples are on GitHub [here](https://github.com/romland/wurm-cinematics-docs/tree/main/examples), 
its repository (together with templates) are [here](https://github.com/romland/wurm-cinematics-docs)

*Note: I do not know whether this mod works on latest version of Wurm Unlimited. In general, though, pretty small patches are needed for upgrades.*

## Example Clips
I dug up a few clips from 2018 that was taken during development...

[![Example Video](https://img.youtube.com/vi/a7q-6sedZuQ/0.jpg)](https://www.youtube.com/watch?v=a7q-6sedZuQ "Example Video")
[![Example Video](https://img.youtube.com/vi/BVRQpaHasQU/0.jpg)](https://www.youtube.com/watch?v=BVRQpaHasQU "Example Video")
[![Example Video](https://img.youtube.com/vi/ne2sDgJlBbo/0.jpg)](https://www.youtube.com/watch?v=ne2sDgJlBbo "Example Video")
[![Example Video](https://img.youtube.com/vi/jrRl3eoWSSc/0.jpg)](https://www.youtube.com/watch?v=jrRl3eoWSSc "Example Video")
[![Example Video](https://img.youtube.com/vi/5C4oKmglCbY/0.jpg)](https://www.youtube.com/watch?v=5C4oKmglCbY "Example Video")


`Below is the original README file that was distributed with the mod.`

[![There is even a logo](https://raw.githubusercontent.com/romland/wurm-cinematics-site/main/image/logo.png)]

# Introduction

   You spend dozens, maybe hundreds or even thousands of hours creating your 
   homestead, village, island ... world. 

   And then you want to show your friends what YOU see and out comes ... rather 
   dull looking videos. Frustrating.

   The goal with this is to make the videos of Wurm more cinematic. Show off
   your creations in style. Maybe even record real stories. With actors.

   But maybe... hopefully... even sell more people on Wurm ... with us all 
   releasing good looking videos.

   This is a client mod for Wurm Unlimited.


#   Getting started
   A lot of time went into developing this software, take your time exploring
   it. You'll likely enjoy it... I hope.
   
   But PLEASE, at least browse the content below (there really are IMPORTANT
   things there).
   
   There are a lot of examples, over 50, in fact. These should cover quite a 
   bit of the functionality available but I am sure I have missed some things
   which I'll have to revisit later. Do note that while the examples are good
   at demonstration, they do severely lack in creativity. I'll be cheesy and 
   say that that bit is left to you.

   There are a few things that the examples are unable to convey, the most 
   important of those things are outlined below. From these pointers you 
   should be able to get around fine.


#   Installing
   1. Unzip the downloaded zip in your Wurm client's folder.
      In your mods folder you should now have *four* new items:
      - friyas-cinematics (folder)
      - friyas-cinematics.properties
      - gson-2.8.0.jar
      - jl1.0.1.jar
      - SpectatorCommunicator.jar

   2. You need to copy (or move) gson-2.8.0.jar, jl.1.0.1.jar and 
      SpectatorCommunicator.jar to the lib folder of your game. You can find 
      this folder at the same level as your mods folder. 
      I.e: Steam\steamapps\common\Wurm Unlimited\WurmLauncher\lib

   3. Start game.


#   Keybinds
   Really, I cannot stress this enough. You WILL want keybinds for a lot of 
   things, but there's one thing that is actually very crucial:
   
   Stop script. You really want to bind it.

   I do suggest binding more keys to make your life easier, but "Stop Script"
   is the most important one in case you find yourself without HUD and a 
   scene running for way longer than you want it to.

   Get bdew's "bind any action" mod and install it from here:
   `https://github.com/bdew-wurm/action`

   Open Wurm console (usually F1, I believe) and type:
   ```
   > act_show on (for future reference in case you want to bind more keys)
   > bind 8 "act 30551 selected"     (this will bind "stop script" to the '8' key).
   ```
   
   But NOTE: for it to work you first have to click an object so something 
   is in the select bar.


#   Cinematics Tab
   Everything around cinematics will end up in this chat-tab. If you are 
   creating videos, this is the tab you want to have open. 
   
   At the time of writing (24 Jun 2018) there is a bug around this, you will
   not visually get an indicator that the tab is open when you first start
   up the game. I'll get around to fixing it at some point. :)

   Closing the Cinematics chat tab will effectively disable the mod and 
   remove all features it may provide (such as location, no own-body etc).


#   Cinematics Menu
   Right click on any object in the game world, the very last menu entry is
   called "Friya's Cinematics". You'll find the basic operations here.


#   The Examples
   The examples are normal text files on your computer (more on that later),
   but they can of course also also be found inside Wurm:
   
   Load examples from the "Load Script" dialog in the Cinematics menu.
   
   If you are more into typing, you can do:
   `/loadscript examples/name-of-example`
   
   Likewise, if you are browsing the examples, you have the convenience
   commands (can be used in any chat-tab):
   `/example next and /example previous`
   
   To bind these two to keys enter the following in your Console:
   ```
   > bind Numpad4 "example previous"
   > bind Numpad6 "example next"
   ```
   Will then bind to numpad's 4 and 6 keys.

   To run a loaded script, go to Cinematics menu and choose:
   Run script

   To stop a script, use the menu or your keybind (! see above).


##   The Scene Designer
   In the Cinematics menu you'll find a sub-menu called "Scene Designer".
   Here you can create quick (but simple) scenes and run them, and from there on 
   refine (see "Creating your own scripts" below).
   
   A script you are creating in Scene Designer is automatically "loaded", so 
   when you do "run script" it will always use the current version. There is 
   never a need to reload the script after changes if it is the currently loaded 
   script.

   The name "Scene Designer" really implies a lot more than what it is. It is 
   more there to make you familiar with functionality than it is to design 
   real scenes. I find myself often using it to create a base with which I 
   expand upon, though. For instance, creating move waypoints and then edit
   manually.

   All scenes created using Scene Designer will last for 10 seconds. This may
   not be what you want, you should edit the saved script to change it. If you 
   move over a large distance in a short time, you will likely be kicked out 
   by an unmodded server.


##   Examples part deux
   Once you've gotten the hang of things, you will NO DOUBT want to work with 
   your scripts manually.
      
   You can find all the relevant files here:
   [Steam Folder]\Wurm Unlimited\WurmLauncher\mods\friyas-cinematics\Cinematics\
   
   Inside this folder you should find a few sub-folders: audio, effects, 
   examples and templates. The one that is probably the most interesting to you 
   right now is the examples folder.
   
   Changes made to scripts on your harddisk will automatically be loaded when
   you run them in game. There is no need to reload scripts after changes.


##   Knowing your location
   You don't need to install any third party mods to see where you are, just 
   right click your character portrait and select "Toggle name display" and you
   will see your coordinate and facing direction.


##   Creating your own scenes, scripts...
   Remember that everything is line-based. A command (e.g. /camera...) must 
   be on one line, a waypoint on another, target on another.*

   In the cinematics menu you have "Edit Script", this will open up your 
   system text editor -- edit away. You do not have to reload your script in
   the game -- it will always use latest saved version.

   To create a script from scratch, simply save it in the Cinematics folder and
   it will appear when you open "Load Script" or use `/loadscript` in the game.
   
   If you want to reference audio from your scripts, place the files in the 
   audio folder.

   The templates folder contains the basis of scripts created using the in-game
   "Scene Designer". This functionality is a bit lacking at the moment, but I'm
   not sure where to take it from here yet.

   Remember: Finding and creating the good shot takes time -- at least for me.

   * There is/was a plan to also make this console-command based so you could bind
     movements to keys with e.g. "/camera move for 1s: target +1 0 0;". But I
     have not found enough *compelling* reasons to do so yet.


##   Useful other mods
   On your SERVER you probably want to install my mod called "GM Goto" or when
   available the "Spectator" mod.
   
   On CLIENT side, other than Bdew's action mod, you may want to install the 
   timelock mod, also by Bdew.
   
   If I get the inspiration I will likely add similar functionality here to
   make it a bit easier (and more predictable) to use.


##   F10 to toggle HUD
   In case you did not know, or forgot. F10 toggles your HUD. There's one 
   exception here, it will not work if you have enabled the benchmarking
   option.
   
   So, beware.


##   Shutting down Wurm
   This mod removes the "Are you sure you want to quit?" question. If you press 
   alt+f4 or exit in some other way it wil immediately exit your client. 
   
   Yes. I have been waging war with that question.


##   Mod options
   The .properties file has some options you can toggle. At the time of writing
   this is not documented here but knowing me, I've probably written comments
   at every option in there. Maybe.



   That's it. For now.

   If you enjoy this, please let me know. It's good for inspiration...
   
   Cheers,
   Friya
