#!/bin/sh -e

template_dir=${MT_SYS_ROOT}/templates
config_dir=${MT_SYS_ROOT}/etc/mineraltree/app

# USAGE: replaceToken CONTENT TEXT REPLACEMENT
#
# Replaces all occurrences of 'TEXT' in 'CONTENT' with 'REPLACEMENT'
# and emits the converted CONTENT
# ---
replaceToken() {
    content=$1; shift
    key=$1; shift
    value=$1; shift

    # Escape '/' characters
    value=$(echo "$value" | sed 's!/!\\/!g')
    echo "$content" | sed "s/{{$key}}/$value/g"
}

# USAGE: applySettings CONTENT PARAM_FILE
#
# Given 'PARAM_FILE' a file containing a list of 'key=value' pairs,
# will iterate over all the pairs and apply a string substitution
# on the 'CONTENT' string replacing 'key' with 'value'. The converted
# 'CONTENT' string is emitted
# ---
applySettings() {
    content=$1; shift
    parameterFile=$1; shift

    while read -r paramLine ; do
        if echo "$paramLine" | grep -qE "^[^#]+=" ; then
            key=$(echo "$paramLine" | sed -E 's/=.*//')
            value=$(echo "$paramLine" | sed  -E 's/^.*=\"(.*)\"$/\1/')
            content=$(replaceToken "$content" "$key" "$value")
        fi
    done < "$parameterFile"
    echo "$content"
}

# USAGE: installTemplate PATH
#
# With PATH as the name of a file to be generated, will read the
# source file from the template directory, apply any string
# replacements requested, and write the resulting file to the
# desired location. The location of the file beneath the template
# directory corresponds to the final location of the transformed
# file. For example the file "${TEMPLATE_DIR}/etc/mt.xml" will be
# installed to "/etc/mt.xml"
# ---
installTemplate() {
    workFile=$1; shift

    content=$(cat "$template_dir/$workFile")
    for config in "$config_dir"/*; do
        content=$(applySettings "$content" "$config")
    done

    mkdir -p "$(dirname "${MT_SYS_ROOT}/$workFile")"
    echo "$content" > "${MT_SYS_ROOT}/$workFile"
}

for template in $(cd "$template_dir"; find . -type f | cut -c 3-); do
    installTemplate "$template"
    echo "Generated $template"
done


# Now that all the templates have been rendered, run the actual command
exec "$@"
