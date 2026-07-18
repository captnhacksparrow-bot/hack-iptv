import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    old_logic = """                val validCategories = remember(categories) { categories.filter { it != "All" } }
                if (validCategories.isEmpty()) {"""
                
    new_logic = """                val validCategories = remember(categories) { categories.filter { it != "All" } }
                if (validCategories.isEmpty() || (validCategories.size == 1 && validCategories[0] == "Favorites")) {"""

    content = content.replace(old_logic, new_logic)
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
