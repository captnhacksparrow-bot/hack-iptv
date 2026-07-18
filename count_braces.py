import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.readlines()
    
    start_line = 533
    
    for i in range(start_line - 1, len(content)):
        if "when (activeTab) {" in content[i]:
            start_line = i + 1
            break
            
    print(f"Found when block at line {start_line}")
    open_braces = 0
    for i in range(start_line - 1, len(content)):
        line = content[i]
        open_braces += line.count('{')
        open_braces -= line.count('}')
        if open_braces == 0:
            print(f"Ends at line {i + 1}")
            for j in range(max(start_line - 1, i - 10), min(len(content), i + 2)):
                print(f"{j+1}: {content[j].strip()}")
            break

if __name__ == "__main__":
    main()
