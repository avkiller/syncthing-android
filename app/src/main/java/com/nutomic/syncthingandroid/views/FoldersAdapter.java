package com.nutomic.syncthingandroid.views;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
// import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.databinding.ItemFolderListBinding;
import com.nutomic.syncthingandroid.model.CachedFolderStatus;
import com.nutomic.syncthingandroid.model.Folder;
import com.nutomic.syncthingandroid.model.FolderStatus;
import com.nutomic.syncthingandroid.service.Constants;
import com.nutomic.syncthingandroid.service.RestApi;
import com.nutomic.syncthingandroid.service.SyncthingService;
import com.nutomic.syncthingandroid.util.FileUtils;
import com.nutomic.syncthingandroid.util.Util;

import java.util.Map;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Generates item views for folder items.
 */
public class FoldersAdapter extends ArrayAdapter<Folder> {

    // private static final String TAG = "FoldersAdapter";

    private final Context mContext;

    private RestApi mRestApi;

    public FoldersAdapter(Context context) {
        super(context, 0);
        mContext = context;
    }

    public void setRestApi(RestApi restApi) {
        mRestApi = restApi;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ItemFolderListBinding binding = (convertView == null)
                ? DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.item_folder_list, parent, false)
                : DataBindingUtil.bind(convertView);

        Folder folder = getItem(position);
        binding.label.setText(TextUtils.isEmpty(folder.label) ? folder.id : folder.label);
        binding.directory.setText(getShortPathForUI(folder.path));
        binding.override.setOnClickListener(view -> { onClickOverride(view, folder); } );
        binding.revert.setOnClickListener(view -> { onClickRevert(view, folder); } );
        binding.openFolder.setOnClickListener(view -> { FileUtils.openFolder(mContext, folder.path); } );

        // Update folder icon.
        int drawableId = R.drawable.baseline_folder_24;
        switch (folder.type) {
            case Constants.FOLDER_TYPE_RECEIVE_ENCRYPTED:
                drawableId = R.drawable.outline_lock_24;
                break;
            case Constants.FOLDER_TYPE_RECEIVE_ONLY:
                drawableId = R.drawable.ic_folder_receive_only;
                break;
            case Constants.FOLDER_TYPE_SEND_ONLY:
                drawableId = R.drawable.ic_folder_send_only;
                break;
            default:
        }
        binding.openFolder.setImageResource(drawableId);

        updateFolderStatusView(binding, folder);
        return binding.getRoot();
    }

    private void updateFolderStatusView(ItemFolderListBinding binding, Folder folder) {
        if  (mRestApi == null || !mRestApi.isConfigLoaded()) {
            binding.conflicts.setVisibility(GONE);
            binding.lastItemFinishedItem.setVisibility(GONE);
            binding.lastItemFinishedTime.setVisibility(GONE);
            binding.items.setVisibility(GONE);
            binding.override.setVisibility(GONE);
            binding.progressBar.setVisibility(GONE);
            binding.revert.setVisibility(GONE);
            binding.state.setVisibility(GONE);
            setTextOrHide(binding.invalid, folder.invalid);
            return;
        }

        // mRestApi is available.
        final Map.Entry<FolderStatus, CachedFolderStatus> folderEntry = mRestApi.getFolderStatus(folder.id);
        final FolderStatus folderStatus = folderEntry.getKey();
        final CachedFolderStatus cachedFolderStatus = folderEntry.getValue();

        boolean failedItems = folderStatus.errors > 0;

        long neededItems = folderStatus.needFiles + folderStatus.needDirectories + folderStatus.needSymlinks + folderStatus.needDeletes;
        boolean outOfSync = folderStatus.state.equals("idle") && neededItems > 0;
        boolean overrideButtonVisible = folder.type.equals(Constants.FOLDER_TYPE_SEND_ONLY) && outOfSync;
        binding.override.setVisibility(overrideButtonVisible ? VISIBLE : GONE);

        binding.progressBar.setVisibility(folderStatus.state.equals("syncing") ? VISIBLE : GONE);

        boolean revertButtonVisible = false;
        if (folder.type.equals(Constants.FOLDER_TYPE_RECEIVE_ONLY)) {
            revertButtonVisible = (folderStatus.receiveOnlyTotalItems > 0);
        } else if (folder.type.equals(Constants.FOLDER_TYPE_RECEIVE_ENCRYPTED)) {
            revertButtonVisible = ((folderStatus.receiveOnlyTotalItems - folderStatus.receiveOnlyChangedDeletes) > 0);
        }
        binding.revert.setText(mContext.getString(folder.type.equals(Constants.FOLDER_TYPE_RECEIVE_ONLY) ?
                                    R.string.revert_local_changes :
                                    R.string.delete_unexpected_items
                                ));
        binding.revert.setVisibility(revertButtonVisible ? VISIBLE : GONE);

        binding.state.setVisibility(VISIBLE);
        if (outOfSync) {
            binding.state.setText(mContext.getString(R.string.status_outofsync));
            binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_red));
        } else if (failedItems) {
            binding.state.setText(mContext.getString(R.string.state_failed_items, folderStatus.errors));
            binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_red));
        } else {
            if (folder.paused) {
                binding.state.setText(mContext.getString(R.string.state_paused));
                binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_purple));
            } else {
                switch(folderStatus.state) {
                    case "clean-waiting":
                        binding.state.setText(mContext.getString(R.string.state_clean_waiting));
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_orange));
                        break;
                    case "cleaning":
                        binding.state.setText(mContext.getString(R.string.state_cleaning));
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_blue));
                        break;
                    case "idle":
                        if (folder.getDeviceCount() <= 1) {
                            // Special case: The folder is IDLE and UNSHARED.
                            binding.state.setText(mContext.getString(R.string.state_unshared));
                            binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_orange));
                        } else if (revertButtonVisible) {
                            binding.state.setText(mContext.getString(R.string.state_local_additions));
                            binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_green));
                        } else {
                            binding.state.setText(mContext.getString(R.string.state_up_to_date));
                            binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_green));
                        }
                        break;
                    case "scan-waiting":
                        binding.state.setText(mContext.getString(R.string.state_scan_waiting));
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_orange));
                        break;
                    case "scanning":
                        binding.state.setText(mContext.getString(R.string.state_scanning));
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_blue));
                        break;
                    case "sync-waiting":
                        binding.state.setText(mContext.getString(R.string.state_sync_waiting));
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_orange));
                        break;
                    case "syncing":
                        binding.progressBar.setProgress((int) cachedFolderStatus.completion);
                        binding.state.setText(
                                mContext.getString(
                                    R.string.state_syncing,
                                    (int) cachedFolderStatus.completion
                                )
                        );
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_blue));
                        break;
                    case "sync-preparing":
                        binding.state.setText(mContext.getString(R.string.state_sync_preparing));
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_blue));
                        break;
                    case "error":
                        if (TextUtils.isEmpty(folderStatus.error)) {
                            binding.state.setText(mContext.getString(R.string.state_error));
                        } else {
                            binding.state.setText(mContext.getString(R.string.state_error_message, folderStatus.error));
                        }
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_red));
                        break;
                    case "unknown":
                        binding.state.setText(mContext.getString(R.string.state_unknown));
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_red));
                        break;
                    default:
                        binding.state.setText(folderStatus.state);
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_red));
                }
            }
        }

        showConflictsUI(binding, cachedFolderStatus.discoveredConflictFiles);

        showLastItemFinishedUI(binding, cachedFolderStatus);

        binding.items.setVisibility(folder.paused ? GONE : VISIBLE);
        String itemsAndSize = "\u2211 ";
        itemsAndSize += mContext.getResources()
                .getQuantityString(R.plurals.files, (int) folderStatus.inSyncFiles, folderStatus.inSyncFiles, folderStatus.globalFiles);
        itemsAndSize += " \u2022 ";
        itemsAndSize += mContext.getString(R.string.folder_size_format,
                Util.readableFileSize(mContext, folderStatus.inSyncBytes),
                Util.readableFileSize(mContext, folderStatus.globalBytes));
        binding.items.setText(itemsAndSize);

        setTextOrHide(binding.invalid, folderStatus.invalid);
    }

    private void showConflictsUI(ItemFolderListBinding binding, 
                                        final String[] discoveredConflictFiles) {
        Integer conflictFileCount = discoveredConflictFiles.length;
        if (conflictFileCount == 0) {
            binding.conflicts.setVisibility(GONE);
            return;
        }

        String itemCountAndFirst = "\u26a0 ";
        itemCountAndFirst += mContext.getResources()
                        .getQuantityString(R.plurals.conflicts, (int) conflictFileCount, conflictFileCount);
        itemCountAndFirst += "\n\u292e ";
        itemCountAndFirst += discoveredConflictFiles[0];
        if (conflictFileCount > 1) {
            itemCountAndFirst += "\n\u2026";
        }

        binding.conflicts.setText(itemCountAndFirst);
        binding.conflicts.setVisibility(VISIBLE);
        return;
    }

    private void showLastItemFinishedUI(ItemFolderListBinding binding, 
                                                final CachedFolderStatus cachedFolderStatus) {
        if (TextUtils.isEmpty(cachedFolderStatus.lastItemFinishedAction) ||
                TextUtils.isEmpty(cachedFolderStatus.lastItemFinishedItem) ||
                TextUtils.isEmpty(cachedFolderStatus.lastItemFinishedTime)) {
            binding.lastItemFinishedItem.setVisibility(GONE);
            binding.lastItemFinishedTime.setVisibility(GONE);
            return;
        }
        String finishedItemText = "\u21cc";
        switch (cachedFolderStatus.lastItemFinishedAction) {
            case "delete":
                // (x)
                finishedItemText += " \u2297";
                break;
            case "update":
                // (*)
                finishedItemText += " \u229b";
                break;
            default:
                // !?
                finishedItemText += " \u2049";
        }
        finishedItemText += " " + Util.getPathEllipsis(cachedFolderStatus.lastItemFinishedItem);

        binding.lastItemFinishedItem.setText(finishedItemText);
        binding.lastItemFinishedItem.setVisibility(VISIBLE);

        String finishedItemTime = "\u21cc\u231a";
        finishedItemTime += Util.formatTime(cachedFolderStatus.lastItemFinishedTime);
        binding.lastItemFinishedTime.setText(finishedItemTime);
        binding.lastItemFinishedTime.setVisibility(VISIBLE);

        return;
    }

    private void setTextOrHide(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setVisibility(GONE);
        } else {
            view.setText(text);
            view.setVisibility(VISIBLE);
        }
    }

    private final String getShortPathForUI(final String path) {
        String shortenedPath = path.replaceFirst("/storage/emulated/0", "[int]");
        shortenedPath = shortenedPath.replaceFirst("/storage/[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}", "[ext]");
        shortenedPath = shortenedPath.replaceFirst("/" + mContext.getPackageName(), "/[app]");
        return "\u2756 " + Util.getPathEllipsis(shortenedPath);
    }

    private void onClickOverride(View view, Folder folder) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.override_changes)
                .setMessage(R.string.override_changes_question)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    // Send "Override changes" through our service to the REST API.
                    Intent intent = new Intent(mContext, SyncthingService.class)
                            .putExtra(SyncthingService.EXTRA_FOLDER_ID, folder.id);
                    intent.setAction(SyncthingService.ACTION_OVERRIDE_CHANGES);
                    mContext.startService(intent);
                })
                .setNegativeButton(android.R.string.no, (dialogInterface, i) -> {});
        confirmDialog.show();
    }

    private void onClickRevert(View view, Folder folder) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.revert_local_changes)
                .setMessage(R.string.revert_local_changes_question)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    // Send "Revert local changes" through our service to the REST API.
                    Intent intent = new Intent(mContext, SyncthingService.class)
                            .putExtra(SyncthingService.EXTRA_FOLDER_ID, folder.id);
                    intent.setAction(SyncthingService.ACTION_REVERT_LOCAL_CHANGES);
                    mContext.startService(intent);
                })
                .setNegativeButton(android.R.string.no, (dialogInterface, i) -> {});
        confirmDialog.show();
    }

}
