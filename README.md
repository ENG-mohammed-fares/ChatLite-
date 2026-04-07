

## Features

- User login with username and password (registered by admin)
- Join and leave chat rooms
- Private messaging between users
- Display of online users with real-time status
- User status management (Active, Busy, Away)
- Automatic refresh of rooms and users
- Search functionality within chat messages
- Multi-threaded server for handling multiple clients
- Graphical user interface using Java Swing
- Admin server console for user management and session monitoring

----------------------------------------------------------------------
----------------------------------------------------------------------

## Technologies Used

- Java
- TCP Socket Programming (Persistent Connections)
- Multi-threading
- Java Swing

----------------------------------------------------------------------
----------------------------------------------------------------------

## How to Run

1. Run the Server
   - ServerConsoleGUI.java
   - Go to the "USER MANAGEMENT" panel on the left
   - Enter a username and a password (minimum 8 characters)
   - Click "Create User" the user will appear in the Existing Users list
   - Repeat for each user you want to allow

2. Run the Client
   - ChatClientGUI.java
   - A login dialog will open enter your registered username and password and IP if needed
   - Click "CONNECT"
   - If the username is not registered you will get: "Username not registered. Ask the admin."
   - If the password is wrong you will get: "Wrong password. Try again."

3. Start Chatting
   - JOIN room1
   - Type your message

----------------------------------------------------------------------
----------------------------------------------------------------------

## System Description

- The server listens on port 5000 for incoming client connections
- Each client connects using a TCP persistent socket
- Each client is handled in a separate thread
- The server manages users, rooms, and messages
- Messages are either broadcasted to rooms or sent privately
- Users must be pre-registered by the admin with a password of at least 8 characters
- Tested locally and over a real network using ZeroTier

----------------------------------------------------------------------
----------------------------------------------------------------------

## Supported Commands

- JOIN room
- LEAVE room
- MSG room message
- PM user message
- USERS
- ROOMS
- STATUS state
- QUIT

----------------------------------------------------------------------
----------------------------------------------------------------------

## Notes

- The server must be running before clients connect
- The default port is 5000
- Multiple clients can run on the same machine or across different machines using ZeroTier
- Passwords and user registrations are stored in memory and reset when the server restarts

