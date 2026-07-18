import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    # We need to replace `channel.catchupDays > 0` with `(channel.catchupDays > 0 || !channel.catchupType.isNullOrEmpty())`
    
    content = content.replace("if (isPast && channel.catchupDays > 0)", "if (isPast && (channel.catchupDays > 0 || !channel.catchupType.isNullOrEmpty()))")
    
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
