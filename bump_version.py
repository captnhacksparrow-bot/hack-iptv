import re
with open("app/build.gradle.kts", "r") as f:
    content = f.read()
    
# increment versionCode and versionName
content = re.sub(r'versionCode = 18', 'versionCode = 19', content)
content = re.sub(r'versionName = "18\.0"', 'versionName = "19.0"', content)

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
