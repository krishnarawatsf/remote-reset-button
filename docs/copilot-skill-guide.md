# RESTCONF CRUD – GitHub Copilot Skill

A GitHub Copilot Agent Skill for the **Remote Reset Button** capstone project.
Teaches Copilot everything it needs to generate correct RESTCONF Python and Java code.

---

## What's Inside

```
.github/
├── copilot-instructions.md              ← Always-on project rules for Copilot
└── skills/
    └── restconf-crud/                   ← The Agent Skill
        ├── SKILL.md                     ← Main skill (auto-loaded when relevant)
        ├── references/
        │   ├── mock-server.md           ← Full mock server code
        │   └── java-implementation.md   ← Full Java class implementations
        └── scripts/
            └── scaffold.sh              ← Auto-generates project files
```

---

## How to Install in VS Code

### Step 1 — Copy the `.github` folder into your project

```bash
# If starting fresh:
cp -r .github /your/project/root/

# Or just drop the files into your existing .github/ folder
```

### Step 2 — Enable Custom Instructions in VS Code

1. Open VS Code Settings (`Ctrl+,` / `Cmd+,`)
2. Search for **"Use Instruction Files"**
3. Check the box: `GitHub Copilot Chat > Code Generation: Use Instruction Files`

### Step 3 — Enable Agent Skills

1. Open the Command Palette (`Ctrl+Shift+P`)
2. Run: **Chat: Open Chat Customizations**
3. Confirm `restconf-crud` appears under **Skills**

> **Note:** Agent Skills require GitHub Copilot Individual, Business, or Enterprise plan.
> The `copilot-instructions.md` works with any Copilot plan.

---

## How to Use the Skill

### Automatic (Copilot decides when to load it)
Just describe what you need — Copilot loads the skill when relevant:
```
"Write a Python script that searches for a device by description using RESTCONF"
"Fix my 401 error when calling the RESTCONF API"
"Generate a Java method that PATCHes an interface to enabled=false"
```

### Manual (force-load the skill)
Type `/restconf-crud` in the Copilot Chat input to explicitly invoke it:
```
/restconf-crud generate the mock_server.py file
/restconf-crud show me the PATCH payload for toggling a port
/restconf-crud what's the correct Content-Type header for RESTCONF?
```

---

## Scaffold Your Project in One Command

```bash
# Python project
bash .github/skills/restconf-crud/scripts/scaffold.sh python

# Java project
bash .github/skills/restconf-crud/scripts/scaffold.sh java
```

Creates all files with correct RESTCONF patterns from scratch.

---

## Skill Triggers

Copilot automatically loads `restconf-crud` when you mention:
- RESTCONF, RFC 8040, YANG, ietf-interfaces
- Remote reset button, power cycle, port reset
- find_device, reboot_port, mock RESTCONF server
- Cisco IOS-XE, DevNet sandbox
- GET/PATCH on network interfaces
- 401 / 404 / 415 errors in a network context

---

## Verification

After installing, open Copilot Chat and type:
```
/restconf-crud
```
You should see the skill load and Copilot should respond with RESTCONF-aware guidance.

To verify `copilot-instructions.md` is active, look at the **References** section
at the top of any Copilot Chat response — the file should be listed there.
