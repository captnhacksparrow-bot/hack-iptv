import zipfile
import os

def create_zip(zip_name, files_to_add):
    print(f"Creating {zip_name}...")
    with zipfile.ZipFile(zip_name, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for file in files_to_add:
            if os.path.exists(file):
                zipf.write(file, os.path.basename(file))
                print(f"  Added {file}")
            else:
                print(f"  Warning: {file} does not exist")

def create_project_zip(zip_name):
    print(f"Creating project-wide zip {zip_name}...")
    exclude_dirs = {'.git', '.gradle', 'build', 'app/build', 'local.properties', '.cxx', 'captures'}
    with zipfile.ZipFile(zip_name, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk('.'):
            # Prune directory search
            dirs[:] = [d for d in dirs if d not in exclude_dirs]
            for file in files:
                filepath = os.path.join(root, file)
                # Skip zip files themselves to avoid infinite recursion
                if file.endswith('.zip') or file.endswith('.pyc') or '__pycache__' in filepath:
                    continue
                # Save relative path inside zip
                arcname = os.path.relpath(filepath, '.')
                zipf.write(filepath, arcname)
    print("Project zip created successfully.")

if __name__ == "__main__":
    # 1. Create pem_upload.zip
    create_zip("pem_upload.zip", ["upload_certificate.pem"])
    
    # 2. Create upload_certificate.zip
    create_zip("upload_certificate.zip", ["upload_certificate.pem"])
    
    # 3. Create my-upload-key.zip
    create_zip("my-upload-key.zip", ["my-upload-key.jks", "upload_certificate.pem"])
    
    # 4. Create project.zip of the whole workspace
    create_project_zip("project.zip")
