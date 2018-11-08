#!/bin/bash -ex

notebookS3Locations=$1
target=$2

find . -name 'sync.sh' -type f -delete

printf "#!/bin/bash -ex\n\n" >> sync.sh

for location in ${notebookS3Locations//,/ }
do
  path=${location%%|*}
  includes=""
  
  if [[ $location = *\|* ]]; then
    includes=${location#*|}
  fi
  
  printf "aws s3 sync $path $target" >> sync.sh
  
  if [[ $includes = *[!\ ]* ]]; then
    printf " --exclude \"*\"" >> sync.sh
  fi
  
  for include in ${includes//|/ }
  do
    printf " --include \"$include\"" >> sync.sh
  done
  printf " --delete" >> sync.sh
  printf "\n" >> sync.sh
  
done

chmod u+x sync.sh
bash sync.sh