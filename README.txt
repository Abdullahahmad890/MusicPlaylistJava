MUSIC PLAYLIST MANAGER — JAVA GUI + C++ BACKEND

Files:
- playlist_backend.cpp : C++ circular linked list and playlist operations
- MusicPlaylistGUI.java : Java Swing graphical interface
- README.txt : instructions

Requirements:
- A C++ compiler (g++)
- Java JDK (javac and java)

1. Open PowerShell in this project folder.

2. Compile the C++ backend:
   g++ -std=c++11 playlist_backend.cpp -o playlist_backend.exe

3. Compile the Java frontend:
   javac MusicPlaylistGUI.java

4. Run the application:
   java MusicPlaylistGUI

IMPORTANT:
- Keep playlist_backend.exe and MusicPlaylistGUI.class in this same folder.
- The Java GUI starts the C++ backend automatically.
- The playlist is stored in memory. Closing the application clears the playlist.
- This project simulates selecting/navigating songs; it does not play audio files.
- The Java frontend communicates with the C++ backend through ProcessBuilder and standard input/output.
