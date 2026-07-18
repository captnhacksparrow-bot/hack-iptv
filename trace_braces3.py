with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
    text = f.read()

start = text.find("fun IptvDashboard")
end = text.find("fun ScreenCastDialog", start)
subtext = text[start:end]

count = 0
for i, line in enumerate(subtext.split('\n')):
    c = line.count('{') - line.count('}')
    count += c
    print(f"{i+1:3d} [{count:2d}] : {line}")
