with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
    text = f.read()

# Get IptvDashboard block
start = text.find("fun IptvDashboard")
end = text.find("fun ScreenCastDialog", start)
subtext = text[start:end]

count = 0
for i, char in enumerate(subtext):
    if char == '{':
        count += 1
    elif char == '}':
        count -= 1
    if count < 0:
        lines = subtext[:i].count('\n') + 1
        print(f"Unmatched }} at relative line {lines}")
        break
print(f"Final count: {count}")
