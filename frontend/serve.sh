#!/bin/bash
# Serve frontend locally on port 3000
# Usage: ./serve.sh
cd "$(dirname "$0")"
echo "Frontend serving at http://localhost:3000"
echo "Make sure API Gateway is running at http://localhost:8080"
python3 -m http.server 3000 2>/dev/null || python -m SimpleHTTPServer 3000 2>/dev/null || npx -y serve -l 3000
