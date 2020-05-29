#!/bin/bash

source ./TestRail_Scripts/TestRailFileConfig.sh
source ./TestRail_Scripts/TestRailUserConfig.sh

#Grep lists
LIST="\+[ ]*@TestRail\|\diff \-\-git\|\-[ ]*@TestRail"
AVOID="\-\-\|index"

#Get new test tags
grep -n -A 1 "$LIST" "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$GIT_DIFF_FILE" > "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$NEW_TEST_TAGS_FILE"

#Get test names and add to array

if grep -q 'TestRail' "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$NEW_TEST_TAGS_FILE"; then
	while IFS= read -r LINE; do 
		if echo "$LINE" | grep -q "diff --git"; then
			FILE_NAME=$(echo ${LINE##*/})
		elif echo "$LINE" | grep -q "\-\-\|index"; then 
			AVOID=1
		elif echo "$LINE" | grep -q "\+[ ]*@TestRail"; then
			IS_NEW=1
		elif echo "$LINE" | grep -q "\-[ ]*@TestRail"; then
			IS_NEW=0
		else
			# get rid of leading white spaces
			LINE=${LINE## }
			# get rid of trailing white spaces
			LINE=${LINE%% }
			# get words before parameters
			TEST_LINE=$(echo "$LINE" | cut -d \( -f 1)
			# split into array based on middle spaces
			IFS=' ' read -r -a ARRAY <<< "$TEST_LINE"
			# get last index in array
			TEST_NAME=${ARRAY[(${#ARRAY[@]})-1]}
			# get rid of new line after index
			# testName=$(echo $testName | tr -d '\r')
			if [ $IS_NEW -eq 0 ]; then
				if ! grep -q "$TEST_NAME," "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$MAP_FILE"; then
					echo "Deleted case not found in map."
				elif grep -q "$TEST_NAME,.*, archived" "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$MAP_FILE"; then
					AVOID=1
				else
					sed -i -e "/$TEST_NAME,/ s/$/, archived/" "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$MAP_FILE"
				fi 
			elif [ $IS_NEW -eq 1 ]; then
				if ! grep -q "$TEST_NAME," "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$MAP_FILE"; then
					NEW_TEST=$TEST_NAME
					NEW_TESTS_ARRAY+=($NEW_TEST)
					echo "File: $FILE_NAME, Test Name: $TEST_NAME," >> "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$MAP_FILE"
				elif grep -q "$TEST_NAME,.*, archived" "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$MAP_FILE"; then 
					TEST_LINE=$(grep $TEST_NAME "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$MAP_FILE")
					CASE_ID=$(echo $TEST_LINE | grep -oP '(?<=CaseID: )[0-9]+')
					grep "$TEST_NAME"
					sed -i -e "s/$CASE_ID, archived/$CASE_ID/g" "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$MAP_FILE"
				else
					echo "Duplicate test name."
				fi
			fi
		fi
	done < "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$NEW_TEST_TAGS_FILE"
else
	echo "No changes to test tags."
fi


#Add any new tests to TestRail
if [ "${#NEW_TESTS_ARRAY[@]}" -eq 0 ]; then
    echo "No new tests added to TestRail."
else
	COUNTER=0
	for i in ${NEW_TESTS_ARRAY[@]}; do
		TEST_NAME=$i
		curl "https://onelogininc.testrail.net/index.php?/api/v2/add_case/$SECTION_ID" \
		-o "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$ID_FILE" \
		-H "Content-Type: application/json" \
		-v -u $EMAIL:$KEY \
		-d "{\"title\": \"$TEST_NAME\", 
		\"custom_owner\": $OWNER_ID,
		\"custom_automation_status\": $AUTOMATION_STATUS,
		\"custom_team_owner\": $TEAM_OWNER_ID}"
		ID=`perl -nle'print $& while m{(?<="id":).*?(?=,)}g' $PATH_TO_TESTRAIL_SCRIPTS_FOLDER$ID_FILE`
		echo $ID
		sed -i -e "s/$TEST_NAME,/$TEST_NAME, CaseID: $ID/g" "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$MAP_FILE"
		COUNTER=$((COUNTER+1))
	done
	echo "$COUNTER tests added to TestRail."
	git add "$PATH_TO_TESTRAIL_SCRIPTS_FOLDER$MAP_FILE"
fi
