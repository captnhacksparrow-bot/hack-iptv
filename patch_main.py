import sys
import re

def main():
    with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
        content = f.read()

    old_logic = """            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }"""
    
    new_logic = """            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }"""
    
    content = content.replace(old_logic, new_logic)

    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
