import urllib.request, json
url = "https://api.github.com/repos/Priyanshu459/B-sync_for_android/releases/latest"
req = urllib.request.Request(url)
with urllib.request.urlopen(req) as response:
    data = json.loads(response.read().decode())
    print("Tag:", data.get("tag_name"))
    print("Assets:", [a["name"] for a in data.get("assets", [])])
