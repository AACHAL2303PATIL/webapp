# 2.4.8

# "letterSoundCorrespondence" → "letterSound"
ALTER TABLE `LetterSoundCorrespondenceContributionEvent` DROP COLUMN `letterSound_id`;
ALTER TABLE `LetterSoundCorrespondenceContributionEvent` CHANGE `letterSoundCorrespondence_id` `letterSound_id` bigint(20) NOT NULL;
