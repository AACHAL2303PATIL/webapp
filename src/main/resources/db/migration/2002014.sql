# 2.2.14

# Change timeSpentMs from NULL to NOT NULL
ALTER TABLE `StoryBookContributionEvent` MODIFY `timeSpentMs` bigint(20) NOT NULL;
