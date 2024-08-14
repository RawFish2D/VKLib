# Build
First of all you need:

- Apache Maven build system
- Java JDK 21 (I'm using GraalVM JDK so I recommend to use it as well)

And to build it run this command:
```bash
cd ./VKLib
mvn clean compile package
```
After which `VKLib-1.0-SNAPSHOT.jar` file and `libs` folder with dependencies should appear base project directory.

# Run
To run it use this command from base project directory:
```bash
java -jar VKLib-1.0-SNAPSHOT.jar
```
If run fails with `Exception in thread "main" java.lang.UnsupportedClassVersionError` in console, it means you are running it with older JDK. You need JDK 21 to run this.

If you have multiple JDK's you can include a full path to java binary to be sure you are using the correct one:
```bash
"C:\Program Files\graalvm-jdk-21.0.2+13.1\bin\java.exe" -jar VKLib-1.0-SNAPSHOT.jar
```
Or if you have SDKMAN! (different JDK manager) you can use that, but I don't have it so don't know how to use it.

After you run it a window should open with test scene with colored circles flying from the center.

# Other
Test scene has different key binds:
- R to reset circles positions. 
- V to toggle vsync.
- F to toggle full screen.
- Space to pause/unpase circles movement.
- Q to switch between different types of queue submit. 
