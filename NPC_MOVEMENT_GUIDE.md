### ⚠️ AI generated MD ⚠️

# FancyNPCs - Complete Movement System Guide

## Table of Contents
- [Overview](#overview)
- [Quick Start](#quick-start)
- [Path Management](#path-management)
- [Position Management](#position-management)
- [Movement Settings](#movement-settings)
- [Advanced Features](#advanced-features)
- [Actions](#actions)
- [Permissions](#permissions)
- [Examples](#examples)
- [Technical Details](#technical-details)

---

## Overview

The FancyNPCs Movement System allows you to create complex, intelligent NPC movement with:
- **Multiple named paths** per NPC
- **Three movement modes**: Continuous, Guide (step-by-step), and Manual
- **Three rotation modes**: Smooth, On Arrival, and None
- **Wait times** at specific positions
- **Action triggers** when reaching positions
- **Per-segment speed control**
- **Follow distance** for guided tours
- **Automatic stuck detection** and recovery
- **Full persistence** - paths save and reload with the server

---

## Quick Start

### Basic Patrol Route
```
1. Create an NPC: /npc create Patrol
2. Add positions: /npc movement Patrol add_position
   (Walk to each patrol point and run this command)
3. Enable looping: /npc movement Patrol loop true
4. Start movement: /npc action Patrol ANY_CLICK add start_movement
5. Click the NPC to start patrolling!
```

### Simple Tour Guide
```
1. Create an NPC: /npc create Guide
2. Add tour stops: /npc movement Guide add_position
   (At entrance, point 1, point 2, etc.)
3. Set to guide mode: /npc movement Guide mode GUIDE
4. Add interaction: /npc action Guide RIGHT_CLICK add next_position
5. Click to move to each stop!
```

---

## Path Management

NPCs support **multiple named paths** - useful for different routes, day/night patrols, or seasonal tours.

### Create a New Path
**Command:** `/npc movement <npc> create_path <path_name>`
**Permission:** `fancynpcs.command.npc.movement.createPath`

**Example:**
```
/npc movement Guard create_path morning_patrol
/npc movement Guard create_path night_patrol
/npc movement Merchant create_path market_route
```

### Select Active Path
**Command:** `/npc movement <npc> select_path <path_name>`
**Permission:** `fancynpcs.command.npc.movement.selectPath`

**Example:**
```
/npc movement Guard select_path night_patrol
/npc action Guard RIGHT_CLICK add start_movement night_patrol
```

### List All Paths
**Command:** `/npc movement <npc> list_paths`
**Permission:** `fancynpcs.command.npc.movement.listPaths`

**Output Example:**
```
==================== Movement Paths ====================
Current: morning_patrol

 ✓ morning_patrol: 5 positions
   night_patrol: 8 positions
   emergency_route: 3 positions

----------------- Total 3 paths -----------------
```

### Remove a Path
**Command:** `/npc movement <npc> remove_path <path_name>`
**Permission:** `fancynpcs.command.npc.movement.removePath`

**Note:** Cannot remove the "default" path.

**Example:**
```
/npc movement Guard remove_path old_route
```

---

## Position Management

Positions define where the NPC moves along the path.

### Add Position
**Command:** `/npc movement <npc> add_position [x] [y] [z] [yaw] [pitch]`
**Permission:** `fancynpcs.command.npc.movement.addPosition`

**Usage:**
- **No arguments** - Uses your current location and rotation
- **With coordinates** - Specify exact position

**Examples:**
```
# Add your current position
/npc movement Guard add_position

# Add specific coordinates
/npc movement Guard add_position 100 64 -200 90 0

# Add with custom rotation
/npc movement Guard add_position 150 65 -150 -180 15
```

### Remove Position
**Command:** `/npc movement <npc> remove_position <index>`
**Permission:** `fancynpcs.command.npc.movement.removePosition`

**Example:**
```
/npc movement Guard remove_position 3
```

### Update Position
**Command:** `/npc movement <npc> set_position <index> [x] [y] [z] [yaw] [pitch]`
**Permission:** `fancynpcs.command.npc.movement.setPosition`

**Examples:**
```
# Update position 2 to your location
/npc movement Guard set_position 2

# Update just the coordinates
/npc movement Guard set_position 2 100 64 -200

# Update coordinates and rotation
/npc movement Guard set_position 2 100 64 -200 90 0
```

### Add Waypoint
**Command:** `/npc movement <npc> add_waypoint [x] [y] [z] [yaw] [pitch]`
**Permission:** `fancynpcs.command.npc.movement.addWaypoint`

**Waypoints** are positions the NPC passes through WITHOUT stopping. Use them to navigate around obstacles between regular positions.

**Examples:**
```
# Add waypoint at current location
/npc movement Guard add_waypoint

# Add waypoint at specific coordinates
/npc movement Guard add_waypoint 100 65 -180
```

**Use Cases:**
- Navigate around walls or obstacles
- Create curved paths
- Avoid obstacles between two positions

### Toggle Position/Waypoint
**Command:** `/npc movement <npc> toggle_waypoint <index>`
**Permission:** `fancynpcs.command.npc.movement.toggleWaypoint`

Convert an existing position to a waypoint or vice versa.

**Example:**
```
# Convert position 2 to a waypoint
/npc movement Guard toggle_waypoint 2

# Convert waypoint 2 back to a position
/npc movement Guard toggle_waypoint 2
```

### Clear All Positions
**Command:** `/npc movement <npc> clear`
**Permission:** `fancynpcs.command.npc.movement.clear`

**Example:**
```
/npc movement Guard clear
```

### List Positions
**Command:** `/npc movement <npc> list`
**Permission:** `fancynpcs.command.npc.movement.list`

**Output Example:**
```
================ Movement Path: default ================
Pace: WALK | Loop: true | Mode: CONTINUOUS | Rotation: SMOOTH

 1. X: 100.50, Y: 64.00, Z: -200.30, Yaw: 90.0, Pitch: 0.0
 2. [WAYPOINT] X: 125.00, Y: 64.00, Z: -175.00, Yaw: 135.0, Pitch: 0.0
 3. X: 150.20, Y: 64.00, Z: -200.30, Yaw: 180.0, Pitch: 0.0

----------------- Showing total of 3 positions -----------------
```

---

## Movement Settings

### Movement Pace (Speed & Visual Animation)
**Command:** `/npc movement <npc> pace <pace>`
**Permission:** `fancynpcs.command.npc.movement.pace`

**Available Paces:**
- `WALK` - Normal walking speed (0.2 blocks/tick) - Standing pose
- `SNEAK` - Sneaking speed (0.1 blocks/tick) - **Crouching/sneaking animation**
- `SPRINT` - Sprinting speed (0.28 blocks/tick) - **Sprinting animation**
- `SPRINT_JUMP` - Sprint with jumping (0.3 blocks/tick) - **Sprinting animation**
- `SWIM` - Swimming speed (0.15 blocks/tick) - **Swimming animation**

> **Note:** Pace controls both movement speed AND visual animations! NPCs will actually display sneaking, sprinting, or swimming poses based on the selected pace.

**Examples:**
```
/npc movement Guard pace WALK
/npc movement Courier pace SPRINT          # Will sprint and show sprinting animation
/npc movement Merchant pace SNEAK          # Will crouch and show sneaking animation
```

### Loop Mode
**Command:** `/npc movement <npc> loop <true|false>`
**Permission:** `fancynpcs.command.npc.movement.loop`

**Examples:**
```
# Continuous patrol
/npc movement Guard loop true

# One-time journey
/npc movement Escort loop false
```

### Rotation Mode
**Command:** `/npc movement <npc> rotation_mode <mode>`
**Permission:** `fancynpcs.command.npc.movement.rotationMode`

**Modes:**
- `SMOOTH` - Head rotates while walking toward target (default)
- `ON_ARRIVAL` - Head only rotates when reaching position
- `NONE` - Head never rotates during movement

**Examples:**
```
# Natural walking (looks where moving)
/npc movement Guard rotation_mode SMOOTH

# Snap rotation at each point
/npc movement Sentry rotation_mode ON_ARRIVAL

# Fixed gaze direction
/npc movement Statue rotation_mode NONE
```

### Movement Mode
**Command:** `/npc movement <npc> mode <mode>`
**Permission:** `fancynpcs.command.npc.movement.mode`

**Modes:**
- `CONTINUOUS` - Automatically moves through all positions
- `GUIDE` - Waits at each position for player interaction to continue
- `MANUAL` - Only moves when triggered by action

**Examples:**
```
# Auto-patrol
/npc movement Guard mode CONTINUOUS

# Tour guide (wait for player clicks)
/npc movement TourGuide mode GUIDE

# Quest-driven movement
/npc movement QuestNPC mode MANUAL
```

---

## Advanced Features

### Wait Times at Positions
**Command:** `/npc movement <npc> set_wait <index> <seconds>`
**Permission:** `fancynpcs.command.npc.movement.setWait`

Makes the NPC pause at a specific position before continuing.

**Examples:**
```
# Wait 5 seconds at position 1
/npc movement Guard set_wait 1 5.0

# Wait 10 seconds at position 3
/npc movement Guard set_wait 3 10.0

# Remove wait time (set to 0)
/npc movement Guard set_wait 2 0
```

**Use Cases:**
- Guards stopping at checkpoints
- Tour guides explaining points of interest
- NPCs "looking around" before moving on

### Action Triggers at Positions (Execute on Arrival)
**Command:** `/npc movement <npc> set_action <index> <trigger>`
**Permission:** `fancynpcs.command.npc.movement.setAction`

**Automatically execute NPC actions when the NPC arrives at a specific position.** This is perfect for guide mode commentary, quest progression, or environmental storytelling.

**Triggers:**
- `ANY_CLICK` - Execute actions for all players when NPC reaches position
- `LEFT_CLICK` - Execute on left click
- `RIGHT_CLICK` - Execute on right click
- `CUSTOM` - Execute via API

> **Important:** Actions execute automatically when the NPC arrives at the position - no player interaction needed! This works in all movement modes (CONTINUOUS, GUIDE, and MANUAL).

**Examples:**
```
# Send message when reaching position 1
/npc movement Guard set_action 1 ANY_CLICK
/npc action Guard ANY_CLICK add message Checkpoint Alpha secured.

# Play sound at position 2
/npc movement Musician set_action 2 ANY_CLICK
/npc action Musician ANY_CLICK add play_sound minecraft:music_disc.cat

# Give item at position 3
/npc movement Merchant set_action 3 ANY_CLICK
/npc action Merchant ANY_CLICK add player_command give @s diamond 1
```

**Use Cases:**
- Tour guide commentary at each stop
- Quest item delivery
- Environmental storytelling
- Checkpoint notifications

### Position-Specific Actions (Multiple Actions per Position)
**Commands:**
- `/npc movement <npc> add_position_action <index> <action> [value]` - Add action to position
- `/npc movement <npc> remove_position_action <index> <action_index>` - Remove specific action
- `/npc movement <npc> clear_position_actions <index>` - Clear all actions at position
- `/npc movement <npc> list_position_actions <index>` - List all actions at position

**Permissions:**
- `fancynpcs.command.npc.movement.addPositionAction`
- `fancynpcs.command.npc.movement.removePositionAction`
- `fancynpcs.command.npc.movement.clearPositionActions`
- `fancynpcs.command.npc.movement.listPositionActions`

**Add multiple different actions directly to specific positions without needing separate triggers.** This is the recommended way to set up position actions, especially when you have many positions or multiple actions per position.

**Available Actions:**
- `message <text>` - Send message to all online players
- `server_command <command>` - Execute server command
- `player_command <command>` - Execute command as each player
- `play_sound <sound>` - Play sound effect
- `action_bar <text>` - Send action bar message
- `title <title>` - Send title message
- And more... (see `/npc action` for full list)

**Examples:**
```
# Add multiple messages to position 1
/npc movement TourGuide add_position_action 1 message Welcome to the museum!
/npc movement TourGuide add_position_action 1 message This tour will take about 10 minutes.

# Add message and sound to position 2
/npc movement TourGuide add_position_action 2 message This is our newest exhibit.
/npc movement TourGuide add_position_action 2 play_sound minecraft:block.note_block.chime

# Add action bar and particle effect to position 3
/npc movement Guard add_position_action 3 action_bar Checkpoint Alpha
/npc movement Guard add_position_action 3 server_command particle minecraft:flame ~ ~1 ~ 0.5 0.5 0.5 0 20

# List all actions at position 1
/npc movement TourGuide list_position_actions 1

# Remove the second action from position 1
/npc movement TourGuide remove_position_action 1 1

# Clear all actions from position 2
/npc movement TourGuide clear_position_actions 2
```

**Advantages over Trigger-based Actions:**
- **Multiple different actions per position** - No need for separate triggers
- **Easier to manage** - Actions are directly attached to positions
- **Scalable** - Works well even with dozens of positions
- **Independent** - Each position has its own action list

**Use Cases:**
- Multi-step tour guide commentary with multiple messages per stop
- Complex checkpoint systems with sounds, messages, and commands
- Quest progression with multiple actions per stage
- Environmental storytelling with coordinated effects

### Per-Segment Speed Control (with Visual Animations)
**Command:** `/npc movement <npc> set_pace_segment <from> <to> <pace>`
**Permission:** `fancynpcs.command.npc.movement.setPaceSegment`

Set different speeds AND visual animations between specific positions. Each segment can have its own pace with corresponding pose.

**Examples:**
```
# Walk from 1 to 2, sprint from 2 to 3
/npc movement Guard set_pace_segment 1 2 WALK      # Standing pose
/npc movement Guard set_pace_segment 2 3 SPRINT    # Sprinting animation

# Sneak approach, sprint escape
/npc movement Spy set_pace_segment 1 2 SNEAK       # Crouching animation
/npc movement Spy set_pace_segment 2 3 SPRINT_JUMP # Sprinting animation
```

> **Note:** The NPC will automatically switch between visual poses (standing, sneaking, sprinting, swimming) as it moves through different segments!

**Use Cases:**
- Dramatic chase sequences with visual storytelling
- Stealthy approaches (NPC actually crouches)
- Varied patrol patterns with different gaits
- Racing NPCs with realistic sprint animations

### Follow Distance (Guide Mode)
**Command:** `/npc movement <npc> follow_distance <distance>`
**Permission:** `fancynpcs.command.npc.movement.followDistance`

In GUIDE mode, NPC waits if the player is too far away.

**Examples:**
```
# Wait if player is more than 10 blocks away
/npc movement TourGuide follow_distance 10

# Wait if player is more than 20 blocks away
/npc movement Escort follow_distance 20

# Disable (never wait for player)
/npc movement Guide follow_distance -1
```

**Use Cases:**
- Tour guides that wait for slow players
- Escort missions
- Group leaders
- Tutorial NPCs

### Physics (Player-like Movement)
**Commands:**
- `/npc movement <npc> physics <true|false>` - Enable/disable physics for current path
- `/npc movement <npc> physics_global <true|false>` - Enable/disable physics for entire NPC

**Permissions:**
- `fancynpcs.command.npc.movement.physics` - Path-level physics
- `fancynpcs.command.npc.movement.physicsGlobal` - Global NPC physics

When physics is enabled, NPCs behave like players:
- **Gravity** - NPCs fall down if there's no ground
- **Collision** - NPCs can't fly through solid blocks
- **Step-up** - NPCs automatically step up blocks and stairs (1 block height)

> **Note:** Physics applies if EITHER path-level OR global NPC physics is enabled. Disabled by default for backwards compatibility.

**Examples:**
```
# Enable physics for current path only
/npc movement Guard physics true

# Enable physics for entire NPC (all paths)
/npc movement Villager physics_global true

# Disable physics
/npc movement Ghost physics false
```

**Use Cases:**
- Ground-based NPCs that should walk realistically
- NPCs navigating stairs and terrain
- Preventing NPCs from floating or flying through walls
- Creating realistic escort missions

**When to Disable:**
- Flying NPCs (ghosts, spirits, drones)
- NPCs that need to move through walls
- Cinematic sequences with precise positioning
- Performance-critical scenarios

### Path Validation & Stuck Detection
**Automatic Feature** - No commands needed!

The system automatically:
- Checks every 5 seconds if NPC has moved
- If stuck (moved less than 0.5 blocks), teleports to next position
- Continues movement after unsticking

**Benefits:**
- NPCs don't get stuck on terrain
- Handles player-built obstacles
- Ensures reliable movement

---

## Actions

Movement is controlled through the action system. Add these to triggers like `ANY_CLICK`, `LEFT_CLICK`, or `RIGHT_CLICK`.

### Start Movement
**Action:** `start_movement [path_name]`
**Requires Value:** No (path name is optional)
**Permission:** `fancynpcs.command.npc.action.add.start_movement`

**Examples:**
```
# Start current path
/npc action Guard ANY_CLICK add start_movement

# Start specific path
/npc action Guard LEFT_CLICK add start_movement morning_patrol

# Switch paths dynamically
/npc action Guard LEFT_CLICK add start_movement day_route
/npc action Guard RIGHT_CLICK add start_movement night_route
```

### Stop Movement
**Action:** `stop_movement`
**Requires Value:** No
**Permission:** `fancynpcs.command.npc.action.add.stop_movement`

**Examples:**
```
# Stop on click
/npc action Guard ANY_CLICK add stop_movement

# Emergency stop
/npc action Guard LEFT_CLICK add stop_movement
/npc action Guard LEFT_CLICK add message Halted!
```

### Next Position
**Action:** `next_position`
**Requires Value:** No
**Permission:** `fancynpcs.command.npc.action.add.next_position`

**Examples:**
```
# Guide mode - click to advance
/npc movement TourGuide mode GUIDE
/npc action TourGuide RIGHT_CLICK add next_position
/npc action TourGuide RIGHT_CLICK add message Let's continue!

# Manual control
/npc movement QuestNPC mode MANUAL
/npc action QuestNPC RIGHT_CLICK add next_position
```

### Previous Position
**Action:** `previous_position`
**Requires Value:** No
**Permission:** `fancynpcs.command.npc.action.add.previous_position`

**Examples:**
```
# Go back one step
/npc action TourGuide LEFT_CLICK add previous_position
/npc action TourGuide LEFT_CLICK add message Going back...

# Review previous location
/npc action Guide SHIFT_RIGHT_CLICK add previous_position
```

### Return to Start
**Action:** `return_to_start`
**Requires Value:** No
**Permission:** `fancynpcs.command.npc.action.add.return_to_start`

**Examples:**
```
# Reset to beginning
/npc action TourGuide ANY_CLICK add return_to_start
/npc action TourGuide ANY_CLICK add message Back to the start!

# Emergency return
/npc action Guard LEFT_CLICK add return_to_start
/npc action Guard LEFT_CLICK add stop_movement
```

---

## Permissions

### Path Management
- `fancynpcs.command.npc.movement.createPath` - Create new paths
- `fancynpcs.command.npc.movement.selectPath` - Switch between paths
- `fancynpcs.command.npc.movement.removePath` - Delete paths
- `fancynpcs.command.npc.movement.listPaths` - View all paths

### Position Management
- `fancynpcs.command.npc.movement.addPosition` - Add positions
- `fancynpcs.command.npc.movement.removePosition` - Remove positions
- `fancynpcs.command.npc.movement.setPosition` - Update positions
- `fancynpcs.command.npc.movement.addWaypoint` - Add waypoints (pass-through points)
- `fancynpcs.command.npc.movement.toggleWaypoint` - Convert position to waypoint or vice versa
- `fancynpcs.command.npc.movement.clear` - Clear all positions
- `fancynpcs.command.npc.movement.list` - View positions

### Basic Settings
- `fancynpcs.command.npc.movement.pace` - Set movement speed
- `fancynpcs.command.npc.movement.loop` - Toggle looping
- `fancynpcs.command.npc.movement.rotationMode` - Set rotation behavior
- `fancynpcs.command.npc.movement.mode` - Set movement mode

### Advanced Features
- `fancynpcs.command.npc.movement.setWait` - Set wait times
- `fancynpcs.command.npc.movement.setAction` - Set position actions
- `fancynpcs.command.npc.movement.setPaceSegment` - Per-segment speeds
- `fancynpcs.command.npc.movement.followDistance` - Guide follow distance
- `fancynpcs.command.npc.movement.physics` - Path-level physics
- `fancynpcs.command.npc.movement.physicsGlobal` - Global NPC physics

### Actions
- `fancynpcs.command.npc.action.add.start_movement` - Start movement action
- `fancynpcs.command.npc.action.add.stop_movement` - Stop movement action
- `fancynpcs.command.npc.action.add.next_position` - Next position action
- `fancynpcs.command.npc.action.add.previous_position` - Previous position action
- `fancynpcs.command.npc.action.add.return_to_start` - Return to start action

### Permission Wildcards
- `fancynpcs.command.npc.movement.*` - All movement commands
- `fancynpcs.command.npc.action.add.*` - All action additions

---

## Examples

### Example 1: Basic Guard Patrol
```bash
# Create NPC
/npc create Guard

# Set up patrol route
/npc movement Guard add_position  # Position 1
/npc movement Guard add_position  # Position 2
/npc movement Guard add_position  # Position 3
/npc movement Guard add_position  # Position 4

# Configure movement
/npc movement Guard pace WALK
/npc movement Guard loop true
/npc movement Guard mode CONTINUOUS

# Add start trigger
/npc action Guard ANY_CLICK add start_movement
/npc action Guard ANY_CLICK add message Beginning patrol...
```

### Example 2: Interactive Tour Guide
```bash
# Create NPC
/npc create TourGuide

# Create tour path with waypoint to navigate around a fountain
/npc movement TourGuide add_position      # Position 1: Entrance
/npc movement TourGuide add_waypoint      # Waypoint: Navigate around fountain
/npc movement TourGuide add_position      # Position 2: First attraction
/npc movement TourGuide add_position      # Position 3: Second attraction
/npc movement TourGuide add_position      # Position 4: Third attraction
/npc movement TourGuide add_position      # Position 5: Exit

# Configure as guide
/npc movement TourGuide mode GUIDE
/npc movement TourGuide rotation_mode ON_ARRIVAL
/npc movement TourGuide follow_distance 10
/npc movement TourGuide physics_global true

# Add wait times for explanations
/npc movement TourGuide set_wait 1 5
/npc movement TourGuide set_wait 2 10
/npc movement TourGuide set_wait 3 10
/npc movement TourGuide set_wait 4 8

# Add position-specific messages (with colors)
/npc movement TourGuide add_position_action 1 message &6&lWelcome to the tour!
/npc movement TourGuide add_position_action 1 message &ePlease stay close and listen carefully.

/npc movement TourGuide add_position_action 2 message &b&lThis is our first exhibit — &f&oThe Ancient Fountain&b...
/npc movement TourGuide add_position_action 2 play_sound minecraft:block.note_block.harp

/npc movement TourGuide add_position_action 3 message &a&lHere we have the &2Sculpture Garden&a...
/npc movement TourGuide add_position_action 3 message &7Notice the &f&ointricate details&7 on each piece.

/npc movement TourGuide add_position_action 4 message &d&lThis is our newest addition to the museum!
/npc movement TourGuide add_position_action 4 play_sound minecraft:entity.player.levelup

# Optional exit message (position 5)
/npc movement TourGuide add_position_action 5 message &6&lThank you for joining the tour!
/npc movement TourGuide add_position_action 5 message &eWe hope you enjoyed your visit. Have a great day!

# Add player controls for navigation
/npc action TourGuide RIGHT_CLICK add next_position
/npc action TourGuide LEFT_CLICK add previous_position
```

### Example 3: Dramatic Chase Sequence
```bash
# Create NPC
/npc create Spy

# Set up escape route
/npc movement Spy add_position  # Start (hiding)
/npc movement Spy add_position  # Spotted
/npc movement Spy add_position  # Running
/npc movement Spy add_position  # Escape

# Configure speeds
/npc movement Spy pace WALK
/npc movement Spy set_pace_segment 1 2 SNEAK
/npc movement Spy set_pace_segment 2 3 SPRINT
/npc movement Spy set_pace_segment 3 4 SPRINT_JUMP

# Add drama
/npc movement Spy set_action 1 ANY_CLICK
/npc action Spy ANY_CLICK add message &7*creeping quietly*

/npc movement Spy set_action 2 ANY_CLICK
/npc action Spy ANY_CLICK add message &c&lDISCOVERED!
/npc action Spy ANY_CLICK add play_sound minecraft:entity.enderman.scream

/npc movement Spy set_action 4 ANY_CLICK
/npc action Spy ANY_CLICK add message &a*escaped!*

# Start the sequence
/npc action Spy ANY_CLICK add start_movement
```

### Example 4: Multi-Path Merchant
```bash
# Create merchant
/npc create Merchant

# Create market route
/npc movement Merchant create_path market_route
/npc movement Merchant select_path market_route
/npc movement Merchant add_position  # Market stall 1
/npc movement Merchant add_position  # Market stall 2
/npc movement Merchant add_position  # Market stall 3
/npc movement Merchant set_wait 1 15
/npc movement Merchant set_wait 2 15
/npc movement Merchant set_wait 3 15
/npc movement Merchant pace WALK
/npc movement Merchant loop true

# Create warehouse route
/npc movement Merchant create_path warehouse_route
/npc movement Merchant select_path warehouse_route
/npc movement Merchant add_position  # Market to warehouse
/npc movement Merchant add_position  # Warehouse
/npc movement Merchant add_position  # Back to market
/npc movement Merchant set_wait 2 30
/npc movement Merchant pace WALK
/npc movement Merchant loop false

# Add path switching
/npc action Merchant RIGHT_CLICK add start_movement market_route
/npc action Merchant RIGHT_CLICK add message Off to the market!

/npc action Merchant LEFT_CLICK add start_movement warehouse_route
/npc action Merchant LEFT_CLICK add message Restocking from the warehouse...
```

### Example 5: Quest-Driven NPC
```bash
# Create quest NPC
/npc create QuestGiver

# Create quest path
/npc movement QuestGiver add_position  # Village center
/npc movement QuestGiver add_position  # Cave entrance
/npc movement QuestGiver add_position  # Treasure location
/npc movement QuestGiver add_position  # Back to village

# Set to manual mode
/npc movement QuestGiver mode MANUAL
/npc movement QuestGiver rotation_mode SMOOTH

# Step 1: Start quest
/npc action QuestGiver RIGHT_CLICK add message Follow me to the cave!
/npc action QuestGiver RIGHT_CLICK add next_position

# Step 2: At cave
/npc movement QuestGiver set_action 1 ANY_CLICK
/npc action QuestGiver ANY_CLICK add message Here's the cave. I'll wait here.
/npc action QuestGiver ANY_CLICK add need_permission completed.cave.quest
/npc action QuestGiver ANY_CLICK add next_position

# Step 3: Show treasure
/npc movement QuestGiver set_action 2 ANY_CLICK
/npc action QuestGiver ANY_CLICK add message You found it! Let's return.
/npc action QuestGiver ANY_CLICK add next_position

# Step 4: Reward
/npc movement QuestGiver set_action 3 ANY_CLICK
/npc action QuestGiver ANY_CLICK add message Here's your reward!
/npc action QuestGiver ANY_CLICK add player_command give @s diamond 5
/npc action QuestGiver ANY_CLICK add return_to_start
```

### Example 6: Day/Night Patrol
```bash
# Create guard
/npc create NightGuard

# Create day patrol
/npc movement NightGuard create_path day_patrol
/npc movement NightGuard select_path day_patrol
/npc movement NightGuard add_position  # Main gate
/npc movement NightGuard add_position  # East tower
/npc movement NightGuard add_position  # Market
/npc movement NightGuard add_position  # West tower
/npc movement NightGuard pace WALK
/npc movement NightGuard loop true

# Create night patrol
/npc movement NightGuard create_path night_patrol
/npc movement NightGuard select_path night_patrol
/npc movement NightGuard add_position  # Outer wall 1
/npc movement NightGuard add_position  # Outer wall 2
/npc movement NightGuard add_position  # Dark alley
/npc movement NightGuard add_position  # Outer wall 3
/npc movement NightGuard pace SNEAK
/npc movement NightGuard loop true
/npc movement NightGuard rotation_mode SMOOTH

# Use command blocks to switch paths based on time
# /npc action NightGuard CUSTOM add start_movement day_patrol
# /npc action NightGuard CUSTOM add start_movement night_patrol
```

### Example 7: Race Track NPC
```bash
# Create racer
/npc create Racer

# Create race path
/npc movement Racer add_position  # Start line
/npc movement Racer add_position  # Turn 1
/npc movement Racer add_position  # Straight
/npc movement Racer add_position  # Turn 2
/npc movement Racer add_position  # Finish

# Configure for racing
/npc movement Racer pace SPRINT
/npc movement Racer set_pace_segment 1 2 SPRINT_JUMP
/npc movement Racer set_pace_segment 3 4 SPRINT
/npc movement Racer loop false
/npc movement Racer rotation_mode SMOOTH

# Add race events
/npc movement Racer set_action 1 ANY_CLICK
/npc action Racer ANY_CLICK add message &e&lRace starting in 3...
/npc action Racer ANY_CLICK add wait 1
/npc action Racer ANY_CLICK add message &e&l2...
/npc action Racer ANY_CLICK add wait 1
/npc action Racer ANY_CLICK add message &e&l1...
/npc action Racer ANY_CLICK add wait 1
/npc action Racer ANY_CLICK add message &a&lGO!

/npc movement Racer set_action 4 ANY_CLICK
/npc action Racer ANY_CLICK add message &6&lFinished the race!
/npc action Racer ANY_CLICK add play_sound minecraft:entity.player.levelup

# Start race
/npc action Racer ANY_CLICK add start_movement
```

---
