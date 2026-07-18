import sys, re

def main():
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "r") as f:
        content = f.read()

    # Find the main when block and replace everything after `4 -> FavoritesTab...` until the end of the when block.
    # The when block is inside `Box(modifier = Modifier.weight(1f).fillMaxWidth()) {`
    # and `Row(modifier = Modifier.fillMaxSize()) {`
    
    start_str = "4 -> FavoritesTab"
    end_str = "        Spacer(modifier = Modifier.height(16.dp))"
    
    start_idx = content.find(start_str)
    if start_idx != -1:
        end_idx = content.find(end_str, start_idx)
        # We need to preserve the `}` for the when block and the `}` for the Box.
        # Actually it's easier to just replace the whole chunk:
        chunk = content[start_idx:end_idx]
        print("Found chunk to remove:\n", chunk)
        
        # We want to keep the closing braces for `when (activeTab)` and `Box`.
        # The structure is:
        #             }
        #         }
        #         Spacer(...)
        # So we just replace the chunk with `            }\n        }\n`
        
        content = content[:start_idx] + "            }\n        }\n\n" + content[end_idx:]
        
    with open("app/src/main/java/com/example/ui/IptvDashboard.kt", "w") as f:
        f.write(content)

if __name__ == "__main__":
    main()
