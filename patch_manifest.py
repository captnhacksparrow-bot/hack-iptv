import sys

def main():
    with open("app/src/main/AndroidManifest.xml", "r") as f:
        content = f.read()

    # Remove the invalid attribute from application
    content = content.replace('        android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"\n            android:screenOrientation="sensorLandscape">', '        android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation">', 1)

    with open("app/src/main/AndroidManifest.xml", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
