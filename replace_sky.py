import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.readlines()

    start_line = -1
    for i in range(len(content)):
        if "0, 1, 2, 3 -> {" in content[i] and "when (activeTab) {" in content[i-1]:
            start_line = i
            break

    open_braces = 0
    end_line = -1
    for i in range(start_line, len(content)):
        line = content[i]
        open_braces += line.count('{')
        open_braces -= line.count('}')
        if open_braces == 0 and i > start_line:
            end_line = i
            break

    if start_line != -1 and end_line != -1:
        new_content = content[:start_line] + ["                0, 1, 2, 3 -> {\n", "                    ChannelsExplorerTab(viewModel = viewModel, deviceMode = deviceMode)\n", "                }\n"] + content[end_line+1:]
        with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
            f.writelines(new_content)
        print("Success")
    else:
        print("Could not find")

if __name__ == "__main__":
    main()
