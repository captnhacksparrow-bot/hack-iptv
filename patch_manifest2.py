import sys

def main():
    with open("app/src/main/AndroidManifest.xml", "r") as f:
        content = f.read()

    new_permissions = """    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
"""
    content = content.replace('    <uses-permission android:name="android.permission.INTERNET" />', new_permissions)

    with open("app/src/main/AndroidManifest.xml", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
