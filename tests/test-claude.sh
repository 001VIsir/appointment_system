#!/bin/bash
# Simple test to verify claude command works
echo "Testing claude command..."
echo "Say 'hello' and exit." | claude -p --dangerously-skip-permissions
echo ""
echo "Exit code: $?"
