# Long-Running Agent Harness

This harness implements the architecture described in Anthropic's [Effective Harnesses for Long-Running Agents](https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents) article.

## Quick Start

```bash
# Run 5 development iterations
./run-agent-loop.sh 5
```

## Architecture

The harness consists of:

1. **`run-agent-loop.sh`** - Main script that loops Claude Code calls
2. **`feature_list.json`** - JSON list of features with `passes` status
3. **`claude-progress.txt`** - Progress log across sessions
4. **`init.sh`** - Development server startup script
5. **`agent-logs/`** - Directory containing execution logs

## How It Works

Each iteration, Claude Code is instructed to:

1. **Get bearings**: Read `pwd`, git log, and progress file
2. **Check environment**: Run `init.sh` if needed
3. **Pick a task**: Read `feature_list.json`, pick highest priority incomplete feature
4. **Implement**: Write code following existing patterns
5. **Verify**: Test the feature works
6. **Commit**: Commit changes and update progress files

## Files

### feature_list.json

Contains all features to implement:

```json
{
  "id": "FEAT-001",
  "category": "core",
  "priority": 1,
  "description": "User registration and authentication",
  "steps": ["step 1", "step 2", "..."],
  "passes": false
}
```

- Only modify `passes` field (false -> true when complete)
- Never remove or modify test requirements

### claude-progress.txt

Tracks session-by-session progress:

```markdown
## Session 1 - User Authentication
Date: 2024-01-15 10:30:00
Feature: FEAT-001
What was done:
- Created User entity
- Implemented AuthService
Commit: abc1234
```

## Usage

```bash
# Run 10 iterations
./run-agent-loop.sh 10

# Check logs
cat agent-logs/harness-*.log

# Monitor progress
tail -f claude-progress.txt
```

## Configuration

The script passes `--dangerously-skip-permissions` to Claude Code to avoid manual permission prompts. Adjust in `run-agent-loop.sh` if needed.

## Adding New Features

Edit `feature_list.json` to add new features:

```json
{
  "id": "FEAT-011",
  "category": "enhancement",
  "priority": 11,
  "description": "Your new feature",
  "steps": ["step 1", "step 2"],
  "passes": false
}
```
