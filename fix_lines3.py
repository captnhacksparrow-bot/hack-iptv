import sys

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        lines = f.readlines()
        
    # We want to remove the bad insertion
    bad_start = -1
    for i in range(350, 450):
        if "text = when(activeTab) {" in lines[i]:
            bad_start = i
            break
            
    if bad_start != -1:
        # Check if the next line is "                    4 -> FavoritesTab(viewModel)"
        if "FavoritesTab" in lines[bad_start + 1]:
            print("Found bad insertion at", bad_start + 1)
            # Remove 15 lines
            del lines[bad_start+1:bad_start+16]
            
            with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
                f.writelines(lines)
                
if __name__ == "__main__":
    main()
