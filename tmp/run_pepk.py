import pty
import os
import sys
import time

def run():
    keystore_path = "/tmp/my-upload-key.jks"
    pepk_jar = "/tmp/pepk.jar"
    output_zip = "./tmp/pem_upload.zip"
    encryption_key = "user_pubkey.pem"
    
    cmd = [
        "java", "-jar", pepk_jar,
        "--keystore=" + keystore_path,
        "--alias=upload",
        "--output=" + output_zip,
        "--encryptionkey=" + encryption_key,
        "--rsa-aes-encryption"
    ]
    
    pid, fd = pty.fork()
    if pid == 0:
        # Child process
        os.execvp("java", cmd)
    else:
        # Parent process
        # Read from child's terminal and feed passwords
        output = b""
        time.sleep(1)
        
        # Write keystore password
        os.write(fd, b"changeit\n")
        time.sleep(1)
        
        # Write key password
        os.write(fd, b"changeit\n")
        
        # Keep reading until EOF
        while True:
            try:
                data = os.read(fd, 1024)
                if not data:
                    break
                output += data
            except OSError:
                break
                
        print("Child output:")
        print(output.decode(errors='replace'))

if __name__ == "__main__":
    run()
