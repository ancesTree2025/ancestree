# sv

## Installing bun

We are using `bun` package manager in this project. Follow the instructions on https://bun.sh/ to install it.
Run `bun install` to install the dependencies.

## Bun commands

Run `bun run dev` to run the development version of the application. Run `bun run build` to build a production version of the application.
To see a full list of commands run `bun run`.

## Project management

### Conventional commits

We will use conventional commits.

Commits take the following format:
```
type(scope): description
```

type:

* API relevant changes
  * feat Commits, that adds or remove a new feature
  * fix Commits, that fixes a bug
* refactor Commits, that rewrite/restructure your code, however does not change any API behaviour
* perf Commits are special refactor commits, that improve performance
* style Commits, that do not affect the meaning (white-space, formatting, missing semi-colons, etc)
* test Commits, that add missing tests or correcting existing tests
* docs Commits, that affect documentation only
* build Commits, that affect build components like build tool, ci pipeline, dependencies, project version, ...
* ops Commits, that affect operational components like infrastructure, deployment, backup, recovery, ...
* chore Miscellaneous commits e.g. modifying .gitignore

scope is what you're changing i.e. frontend or backend. It is optional - if refactoring whole project omit it. If doesn't apply (e.g. changing README) then omit it.

### Branch naming

Branches are named as follows: `S[JIRA TICKET NUMBER]-description`.

For example: `S8-skeleton`.

If there is no applicable jira ticket then use 0 for the ticket number.