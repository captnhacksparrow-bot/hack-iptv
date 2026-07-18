import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    # Find else if (activeTab in listOf(4, 5, 6, 7, 8, 9, 10)) {
    old_tab_full = """    } else if (activeTab in listOf(4, 5, 6, 7, 8, 9, 10)) {"""
    
    new_tab_full = """    } else if (activeTab in listOf(4, 5, 6, 7, 8, 9, 10)) {
        BackHandler { activeTab = 0 }"""

    content = content.replace(old_tab_full, new_tab_full)

    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
