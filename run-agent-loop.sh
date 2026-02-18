#!/bin/bash
#
# Long-Running Agent Harness Script
# Based on: https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents
#
# Usage: ./run-agent-loop.sh <number_of_iterations>
#
# This script runs Claude Code in a loop, each time asking it to:
# 1. Read the progress file (claude-progress.txt)
# 2. Read the feature list (feature_list.json)
# 3. Pick a pending task and work on it
# 4. Commit changes and update progress
#

set -e

# Colors for logging
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Log file
LOG_DIR="./agent-logs"
LOG_FILE="$LOG_DIR/harness-$(date +%Y%m%d-%H%M%S).log"
PROGRESS_FILE="./claude-progress.txt"

# Create log directory if not exists
mkdir -p "$LOG_DIR"

# Logging functions
log_info() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] [INFO] $1"
    echo -e "${GREEN}${msg}${NC}"
    echo "$msg" >> "$LOG_FILE"
}

log_warn() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] [WARN] $1"
    echo -e "${YELLOW}${msg}${NC}"
    echo "$msg" >> "$LOG_FILE"
}

log_error() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] $1"
    echo -e "${RED}${msg}${NC}"
    echo "$msg" >> "$LOG_FILE"
}

log_section() {
    local msg="============================================================"
    echo -e "${CYAN}${msg}${NC}"
    echo "$msg" >> "$LOG_FILE"
    msg="[$(date '+%Y-%m-%d %H:%M:%S')] $1"
    echo -e "${BLUE}${msg}${NC}"
    echo "$msg" >> "$LOG_FILE"
    msg="============================================================"
    echo -e "${CYAN}${msg}${NC}"
    echo "$msg" >> "$LOG_FILE"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 <number_of_iterations>"
    echo ""
    echo "Arguments:"
    echo "  number_of_iterations  Number of times to run Claude Code (must be >= 1)"
    echo ""
    echo "Example:"
    echo "  $0 5    # Run Claude Code 5 times"
    echo ""
    echo "This script will:"
    echo "  1. Loop <number_of_iterations> times"
    echo "  2. Each iteration calls Claude Code with a standard prompt"
    echo "  3. Claude will read progress, pick a task, implement it, and commit"
    echo "  4. Logs are saved to: $LOG_DIR/"
    exit 1
}

# Validate input
if [ -z "$1" ]; then
    log_error "Missing argument: number_of_iterations"
    show_usage
fi

if ! [[ "$1" =~ ^[0-9]+$ ]] || [ "$1" -lt 1 ]; then
    log_error "Invalid argument: number_of_iterations must be a positive integer"
    show_usage
fi

ITERATIONS=$1

# The prompt that will be passed to Claude Code each time
# This follows the "coding agent" pattern from the Anthropic article
CODING_AGENT_PROMPT='You are a coding agent working on the appointment_system project.

Please follow these steps:

1. **Get your bearings:**
   - Run `pwd` to confirm your working directory
   - Read the git log (last 10 commits) to see recent work: `git log --oneline -10`
   - Read the progress file: `claude-progress.txt` and `progress.json`

2. **Check for init script:**
   - If `init.sh` exists, run it to start the development environment
   - Verify the basic functionality is working

3. **Pick a task:**
   - Read `feature_list.json` and `feature_list.md` to see all features
   - Choose the HIGHEST PRIORITY feature that has `"passes": false`
   - Focus on ONLY ONE feature per session

4. **Implement the feature:**
   - Write clean, well-structured code
   - Follow existing patterns in the codebase
   - Add appropriate tests

5. **Verify the feature works:**
   - Run tests to confirm functionality
   - Do NOT mark a feature as passing unless you have verified it works

6. **Commit and update progress:**
   - Commit your changes with a descriptive message
   - Update `claude-progress.txt` with what you did
   - Update `feature_list.json` and `feature_list.md` ONLY by changing `passes` to `true` for completed features
   - NEVER remove or modify test requirements


IMPORTANT:
- Work incrementally - do not try to implement multiple features at once
- Leave the codebase in a clean state
- Always commit after completing work
- If you find bugs in existing code, fix them before starting new work
-做好git的管理，方便我监督、检查、回滚，文档的主语言为中文
-当你遇到问题时，把问题记录到problems.md里，然后在你解决问题的过程中，把原因、你的思考过程、解决过程等也写进problems.md里，要详细令人信服
'

# Main execution
log_section "Starting Long-Running Agent Harness"
log_info "Configuration:"
log_info "  - Iterations: $ITERATIONS"
log_info "  - Log file: $LOG_FILE"
log_info "  - Progress file: $PROGRESS_FILE"
log_info "  - Working directory: $(pwd)"

# Initialize progress file if it doesn't exist
if [ ! -f "$PROGRESS_FILE" ]; then
    log_info "Creating initial progress file..."
    echo "# Claude Agent Progress Log" > "$PROGRESS_FILE"
    echo "# Started: $(date '+%Y-%m-%d %H:%M:%S')" >> "$PROGRESS_FILE"
    echo "" >> "$PROGRESS_FILE"
    echo "## Session 0 - Initialization" >> "$PROGRESS_FILE"
    echo "Progress file created. Ready for first agent session." >> "$PROGRESS_FILE"
    echo "" >> "$PROGRESS_FILE"
fi

# Check if feature_list.json exists
if [ ! -f "feature_list.json" ]; then
    log_warn "feature_list.json not found!"
    log_warn "You may need to run an initializer agent first to create it."
fi

# Show git status
log_info "Current git status:"
git status --short 2>/dev/null | head -20 >> "$LOG_FILE" || log_warn "Not a git repository or git not available"

# Main loop
for ((i=1; i<=ITERATIONS; i++)); do
    log_section "Iteration $i of $ITERATIONS"

    # Record start time
    START_TIME=$(date +%s)
    log_info "Starting iteration at $(date '+%Y-%m-%d %H:%M:%S')"

    # Show current progress status
    if [ -f "$PROGRESS_FILE" ]; then
        log_info "Last 5 lines of progress file:"
        tail -5 "$PROGRESS_FILE" >> "$LOG_FILE"
    fi

    # Count remaining features (if feature_list.json exists)
    if [ -f "feature_list.json" ]; then
        REMAINING=$(grep -c '"passes": false' feature_list.json 2>/dev/null || echo "unknown")
        COMPLETED=$(grep -c '"passes": true' feature_list.json 2>/dev/null || echo "unknown")
        log_info "Features remaining: $REMAINING, completed: $COMPLETED"
    fi

    log_info "Calling Claude Code..."

    # Call Claude Code with:
    # - --dangerously-skip-permissions: Skip all permission prompts
    # - --allowedTools: Explicitly allow common tools (optional, for more control)
    # - -p: Pass the prompt directly
    # The prompt instructs Claude to work on one feature, test it, and commit

    # Capture both stdout and stderr
    set +e  # Don't exit on error
    claude --dangerously-skip-permissions -p "$CODING_AGENT_PROMPT" 2>&1 | tee -a "$LOG_FILE"
    CLAUDE_EXIT_CODE=${PIPESTATUS[0]}
    set -e  # Re-enable exit on error

    # Record end time and calculate duration
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    MINUTES=$((DURATION / 60))
    SECONDS=$((DURATION % 60))

    if [ $CLAUDE_EXIT_CODE -eq 0 ]; then
        log_info "Iteration $i completed successfully in ${MINUTES}m ${SECONDS}s"
    else
        log_error "Iteration $i failed with exit code $CLAUDE_EXIT_CODE after ${MINUTES}m ${SECONDS}s"
        log_warn "Continuing to next iteration..."
    fi

    # Show git status after this iteration
    log_info "Git status after iteration $i:"
    git status --short 2>/dev/null | head -10 >> "$LOG_FILE" || true

    # Show recent commits
    log_info "Recent commits:"
    git log --oneline -3 2>/dev/null >> "$LOG_FILE" || true

    # Add separator
    echo "" >> "$LOG_FILE"
    echo "----------------------------------------" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"

    # Small delay between iterations (optional)
    if [ $i -lt $ITERATIONS ]; then
        log_info "Waiting 5 seconds before next iteration..."
        sleep 5
    fi
done

log_section "Harness Completed"
log_info "Total iterations: $ITERATIONS"
log_info "Log file saved to: $LOG_FILE"

# Final summary
if [ -f "feature_list.json" ]; then
    REMAINING=$(grep -c '"passes": false' feature_list.json 2>/dev/null || echo "unknown")
    COMPLETED=$(grep -c '"passes": true' feature_list.json 2>/dev/null || echo "unknown")
    log_info "Final feature status - Completed: $COMPLETED, Remaining: $REMAINING"
fi

log_info "Done!"
