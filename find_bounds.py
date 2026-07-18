import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.readlines()

    start_line = 558
    open_braces = 0
    for i in range(start_line, len(content)):
        line = content[i]
        open_braces += line.count('{')
        open_braces -= line.count('}')
        if open_braces == 0 and i > start_line:
            print(f"Ends at line {i + 1}")
            for j in range(max(start_line - 1, i - 10), min(len(content), i + 2)):
                print(f"{j+1}: {content[j].strip()}")
            break

if __name__ == "__main__":
    main()
