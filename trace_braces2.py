with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
    text = f.read()

start = text.find("fun IptvDashboard")
end = text.find("fun ScreenCastDialog", start)
subtext = text[start:end]

lines = subtext.split('\n')
for i, line in enumerate(lines):
    print(f"{i+1:3d}: {line}")
