#!/bin/bash

opts="$*"

PRE_COMMIT_HOOK_URL=https://raw.githubusercontent.com/yangziwen/diff-check/master/hooks/pre-commit
PRE_COMMIT_CHECKSTYLE_HOOK_URL=https://raw.githubusercontent.com/yangziwen/diff-check/master/hooks/pre-commit-checkstyle
PRE_COMMIT_PMD_HOOK_URL=https://raw.githubusercontent.com/yangziwen/diff-check/master/hooks/pre-commit-pmd
CHECKSTYLE_JAR_URL=https://github.com/yangziwen/diff-check/releases/download/0.0.8/diff-checkstyle.jar
PMD_JAR_URL=https://github.com/yangziwen/diff-check/releases/download/0.0.8/diff-pmd.jar

function get_value_from_opts() {
    key="--$1="
    value=`echo "${opts##*${key}}" | awk -F' --' '{print $1}'`
    if [[ ! "$value" =~ ^-- ]]; then
        echo "$value"
        return 0
    fi
    if [[ "$opts" =~ --$1($|\ ) ]]; then
        echo "true"
        return 0
    fi
    echo ""
}

global="`get_value_from_opts global`"

repo_path="`get_value_from_opts repo-path`"

function get_global_hook_path() {
    hook_path="`git config --global --get core.hooksPath`"
    if [[ -z "$hook_path" ]]; then
        git config --global core.hooksPath ~/.githooks
        hook_path="`git config --global --get core.hooksPath`"
    fi
    if [[ ! -d $hook_path ]]; then
        mkdir -p $hook_path
    fi
    echo "$hook_path"
}

function get_repo_hook_path() {
    repo_path="$1"
    if [ -d $repo_path ]; then
        echo "$repo_path/.git/hooks"
    else
        echo ""
    fi
}

function get_hook_path() {
    if [[ "true" == "$global" ]]; then
        echo "`get_global_hook_path`"
    elif [[ -n "$repo_path" ]]; then
        echo "`get_repo_hook_path $repo_path`"
    else
        echo ""
    fi
}

function copy_hook() {
    url=$1
    dest_dir=$2
    filename=${url##*/}
    curl -L -o $dest_dir/$filename $url
    chmod +x $dest_dir/$filename
}

function copy_jar() {
    url=$1
    dest_dir=$2
    filename=${url##*/}
    curl -L -o $dest_dir/$filename $url
}

hook_path="`get_hook_path`"
if [ -n "$hook_path" ]; then
    copy_hook $PRE_COMMIT_HOOK_URL $hook_path
    copy_hook $PRE_COMMIT_CHECKSTYLE_HOOK_URL $hook_path
    copy_hook $PRE_COMMIT_PMD_HOOK_URL $hook_path
    copy_jar $CHECKSTYLE_JAR_URL $hook_path
    copy_jar $PMD_JAR_URL $hook_path
    if [[ "true" == "$global" ]]; then
        git config --global diff-check.checkstyle.enabled true
        git config --global diff-check.pmd.enabled true
    else
        cd $repo_path
        git config diff-check.checkstyle.enabled true
        git config diff-check.pmd.enabled true
        cd -
    fi
    echo "pre-commit hooks are installed to $hook_path successfully!"
    exit 0
fi

echo "Please specify the options correctly"
echo "  --global => install the hooks globally"
echo "  --repo-path=\${the_absolute_path_of_your_git_repository} => install the hooks to the specified git repository"

