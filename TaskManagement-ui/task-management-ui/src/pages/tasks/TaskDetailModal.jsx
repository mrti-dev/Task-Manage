import { useState, useEffect, useCallback, useRef } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  Typography,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Chip,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  Skeleton,
  Snackbar,
  Alert,
} from '@mui/material';
import {
  CloseRounded as CloseIcon,
  DeleteRounded as DeleteIcon,
  SendRounded as SendIcon,
  EditRounded as EditIcon,
  HowToRegRounded as ClaimIcon,
} from '@mui/icons-material';
import { getTaskByIdApi, updateTaskApi, deleteTaskApi } from '../../api/taskApi';
import { getCommentsByTaskApi, createCommentApi } from '../../api/commentApi';
import { taskStatusColors } from '../../utils/constants';
import { useNotification } from '../../contexts/NotificationContext';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import ConfirmDialog from '../../components/common/ConfirmDialog';

dayjs.extend(relativeTime);

const priorityColors = {
  LOW: { bg: '#F1F5F9', text: '#64748B' },
  MEDIUM: { bg: '#DBEAFE', text: '#2563EB' },
  HIGH: { bg: '#FEF3C7', text: '#D97706' },
  URGENT: { bg: '#FEE2E2', text: '#DC2626' },
};

export default function TaskDetailModal({ taskId, open, onClose, onUpdate, onDelete, isOwner, currentUserId }) {
  const { stompSubscribe } = useNotification();
  const stompSubRef = useRef(null);
  const [task, setTask] = useState(null);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editMode, setEditMode] = useState(false);
  const [editForm, setEditForm] = useState({});
  const [editLoading, setEditLoading] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [commentLoading, setCommentLoading] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const fetchTask = useCallback(async () => {
    if (!taskId) return;
    setLoading(true);
    try {
      const [taskRes, commentsRes] = await Promise.all([getTaskByIdApi(taskId), getCommentsByTaskApi(taskId)]);
      const taskData = taskRes?.data?.data ?? taskRes?.data;
      setTask(taskData);
      const loaded = commentsRes?.data?.data ?? commentsRes?.data ?? [];
      setComments(Array.isArray(loaded) ? loaded.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)) : []);
      setEditForm({
        title: taskData?.title || '', description: taskData?.description || '',
        priority: taskData?.priority || 'MEDIUM', status: taskData?.status || 'TODO',
        assigneeId: taskData?.assignee?.id || '', deadline: taskData?.deadline || '',
      });
    } catch {
      setSnackbar({ open: true, message: 'Failed to load task', severity: 'error' });
    } finally { setLoading(false); }
  }, [taskId]);

  useEffect(() => { if (open) fetchTask(); }, [open, fetchTask]);

  useEffect(() => {
    if (stompSubRef.current) { stompSubRef.current.unsubscribe(); stompSubRef.current = null; }
    if (!open || !task?.workspaceId) return;
    stompSubRef.current = stompSubscribe(
      `/topic/workspace/${task.workspaceId}/tasks/${taskId}/comments`,
      (comment) => {
        setComments((prev) => {
          if (prev.some((c) => c.id === comment.id)) return prev;
          return [comment, ...prev];
        });
      }
    );
    return () => {
      if (stompSubRef.current) { stompSubRef.current.unsubscribe(); stompSubRef.current = null; }
    };
  }, [open, task?.workspaceId, taskId, stompSubscribe]);

  const handleSave = async () => {
    setEditLoading(true);
    try {
      const { data: res } = await updateTaskApi(taskId, {
        title: editForm.title, description: editForm.description,
        priority: editForm.priority, status: editForm.status,
        assigneeId: editForm.assigneeId ? Number(editForm.assigneeId) : null,
        deadline: editForm.deadline || null,
      });
      const updated = res?.data ?? res;
      setTask(updated);
      setEditMode(false);
      onUpdate(updated);
      setSnackbar({ open: true, message: 'Task updated', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Update failed', severity: 'error' });
    } finally { setEditLoading(false); }
  };

  const handleDelete = async () => {
    setDeleteLoading(true);
    try {
      await deleteTaskApi(taskId);
      onDelete(taskId);
      setDeleteOpen(false);
      onClose();
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Delete failed', severity: 'error' });
    } finally { setDeleteLoading(false); }
  };

  const handleClaim = async () => {
    if (!currentUserId) return;
    setEditLoading(true);
    try {
      const { data: res } = await updateTaskApi(taskId, { assigneeId: Number(currentUserId) });
      const updated = res?.data ?? res;
      setTask(updated);
      onUpdate(updated);
      setSnackbar({ open: true, message: 'Task claimed', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Claim failed', severity: 'error' });
    } finally { setEditLoading(false); }
  };

  const handleStatusChange = async (newStatus) => {
    setEditLoading(true);
    try {
      const { data: res } = await updateTaskApi(taskId, { status: newStatus });
      const updated = res?.data ?? res;
      setTask(updated);
      onUpdate(updated);
      setSnackbar({ open: true, message: 'Status updated', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Update failed', severity: 'error' });
    } finally { setEditLoading(false); }
  };

  const handleAddComment = async () => {
    if (!commentText.trim()) return;
    setCommentLoading(true);
    try {
      const { data: res } = await createCommentApi({ taskId: Number(taskId), content: commentText });
      setComments((prev) => {
        const newComment = res?.data ?? res;
        if (prev.some((c) => c.id === newComment.id)) return prev;
        return [newComment, ...prev];
      });
      setCommentText('');
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to add comment', severity: 'error' });
    } finally { setCommentLoading(false); }
  };

  const sc = (s) => taskStatusColors[s] || { bg: '#F1F5F9', text: '#64748B' };
  const pc = (p) => priorityColors[p] || { bg: '#F1F5F9', text: '#64748B' };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth
        PaperProps={{ sx: { borderRadius: 4, overflow: 'hidden' } }}>
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 3, pt: 2.5, pb: 1.5, fontWeight: 700, color: '#0F172A' }}>
          {loading ? 'Loading...' : task?.title}
          <Box>
            {!loading && isOwner && (
              <IconButton onClick={() => setDeleteOpen(true)} size="small" sx={{ mr: 0.5, color: '#EF4444' }}>
                <DeleteIcon />
              </IconButton>
            )}
            <IconButton onClick={onClose} size="small" sx={{ color: '#94A3B8' }}>
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <Divider />
        <DialogContent sx={{ pt: 2.5, px: 3 }}>
          {loading ? (
            <Box>
              <Skeleton variant="text" width="60%" height={32} sx={{ borderRadius: 2 }} />
              <Skeleton variant="text" width="40%" height={24} sx={{ mt: 1, borderRadius: 2 }} />
              <Skeleton variant="rounded" height={120} sx={{ mt: 2, borderRadius: 3 }} />
            </Box>
          ) : editMode ? (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <TextField fullWidth label="Title" value={editForm.title}
                onChange={(e) => setEditForm((p) => ({ ...p, title: e.target.value }))} />
              <TextField fullWidth label="Description" value={editForm.description}
                onChange={(e) => setEditForm((p) => ({ ...p, description: e.target.value }))} multiline rows={3} />
              <FormControl fullWidth><InputLabel>Priority</InputLabel>
                <Select value={editForm.priority} label="Priority" onChange={(e) => setEditForm((p) => ({ ...p, priority: e.target.value }))}>
                  <MenuItem value="LOW">Low</MenuItem>
                  <MenuItem value="MEDIUM">Medium</MenuItem>
                  <MenuItem value="HIGH">High</MenuItem>
                  <MenuItem value="URGENT">Urgent</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth><InputLabel>Status</InputLabel>
                <Select value={editForm.status} label="Status" onChange={(e) => setEditForm((p) => ({ ...p, status: e.target.value }))}>
                  <MenuItem value="TODO">To Do</MenuItem>
                  <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                  <MenuItem value="DONE">Done</MenuItem>
                </Select>
              </FormControl>
              <TextField fullWidth type="date" value={editForm.deadline}
                onChange={(e) => setEditForm((p) => ({ ...p, deadline: e.target.value }))} />
              <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
                <Button onClick={() => setEditMode(false)} disabled={editLoading} sx={{ color: '#64748B' }}>Cancel</Button>
                <Button variant="contained" onClick={handleSave} disabled={editLoading}
                  sx={{ borderRadius: 2, background: 'linear-gradient(135deg, #3B82F6, #60A5FA)' }}>
                  {editLoading ? 'Saving...' : 'Save'}
                </Button>
              </Box>
            </Box>
          ) : (
            <Box>
              <Box sx={{ display: 'flex', gap: 1.5, mb: 2.5, flexWrap: 'wrap', alignItems: 'center' }}>
                {isOwner ? (
                  <Chip label={task?.status?.replace('_', ' ')} size="small"
                    sx={{ fontWeight: 600, fontSize: '0.6875rem', bgcolor: sc(task?.status).bg, color: sc(task?.status).text }} />
                ) : (
                  <FormControl size="small" sx={{ minWidth: 130 }}>
                    <Select value={task?.status || 'TODO'} onChange={(e) => handleStatusChange(e.target.value)}
                      disabled={editLoading}
                      sx={{ fontWeight: 600, fontSize: '0.75rem', borderRadius: 2,
                        '& .MuiSelect-select': { py: 0.5 },
                        bgcolor: sc(task?.status).bg, color: sc(task?.status).text,
                        '& .MuiOutlinedInput-notchedOutline': { borderColor: 'transparent' },
                        '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: '#CBD5E1' } }}>
                      <MenuItem value="TODO">To Do</MenuItem>
                      <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                      <MenuItem value="DONE">Done</MenuItem>
                    </Select>
                  </FormControl>
                )}
                <Chip label={task?.priority} size="small"
                  sx={{ fontWeight: 600, fontSize: '0.6875rem', bgcolor: pc(task?.priority).bg, color: pc(task?.priority).text }} />
                {task?.assignee && (
                  <Chip avatar={<Avatar sx={{ width: 22, height: 22, bgcolor: '#3B82F6', fontSize: 11 }}>{task.assignee.username?.[0]?.toUpperCase()}</Avatar>}
                    label={task.assignee.username} size="small" variant="outlined" sx={{ fontWeight: 500, borderColor: '#CBD5E1' }} />
                )}
                {task?.deadline && <Chip label={`Due: ${task.deadline}`} size="small" variant="outlined" sx={{ fontWeight: 500, borderColor: '#CBD5E1' }} />}
              </Box>

              {task?.description && (
                <Typography variant="body2" sx={{ color: '#475569', mb: 3, whiteSpace: 'pre-wrap', lineHeight: 1.7 }}>
                  {task.description}
                </Typography>
              )}

              <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mb: 2 }}>
                {!isOwner && !task?.assignee && currentUserId && (
                  <Button size="small" onClick={handleClaim} disabled={editLoading}
                    startIcon={<ClaimIcon sx={{ fontSize: 16 }} />}
                    sx={{ color: '#10B981', fontWeight: 600, textTransform: 'none' }}>
                    Claim
                  </Button>
                )}
                {isOwner && (
                  <Button size="small" onClick={() => setEditMode(true)}
                    startIcon={<EditIcon sx={{ fontSize: 16 }} />}
                    sx={{ color: '#3B82F6', fontWeight: 600, textTransform: 'none' }}>
                    Edit
                  </Button>
                )}
              </Box>

              <Divider sx={{ mb: 2 }} />
              <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1.5, color: '#0F172A' }}>
                Comments ({comments.length})
              </Typography>

              <Box sx={{ mb: 2, maxHeight: 250, overflow: 'auto' }}>
                {comments.length === 0 ? (
                  <Typography variant="body2" sx={{ py: 3, textAlign: 'center', color: '#94A3B8' }}>No comments yet</Typography>
                ) : (
                  <List disablePadding dense>
                    {comments.map((comment) => (
                      <ListItem key={comment.id} alignItems="flex-start" disableGutters sx={{ px: 0, py: 1 }}>
                        <ListItemAvatar sx={{ minWidth: 40 }}>
                          <Avatar sx={{ width: 32, height: 32, bgcolor: '#3B82F6', fontSize: 12, fontWeight: 700 }}>
                            {comment.user?.username?.[0]?.toUpperCase() || '?'}
                          </Avatar>
                        </ListItemAvatar>
                        <ListItemText
                          primary={
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <Typography variant="subtitle2" fontSize={13} fontWeight={600} sx={{ color: '#0F172A' }}>
                                {comment.user?.username || 'Unknown'}
                              </Typography>
                              <Typography variant="caption" sx={{ color: '#CBD5E1' }}>
                                {dayjs(comment.createdAt).fromNow()}
                              </Typography>
                            </Box>
                          }
                          secondary={<Typography variant="body2" sx={{ color: '#475569', mt: 0.25 }}>{comment.content}</Typography>}
                        />
                      </ListItem>
                    ))}
                  </List>
                )}
              </Box>

              <Box sx={{ display: 'flex', gap: 1, pt: 1, borderTop: '1px solid #F1F5F9' }}>
                <TextField fullWidth size="small" placeholder="Write a comment..."
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleAddComment(); } }} />
                <IconButton color="primary" onClick={handleAddComment} disabled={commentLoading || !commentText.trim()}
                  sx={{ bgcolor: '#F1F5F9', '&:hover': { bgcolor: '#DBEAFE' }, color: '#3B82F6' }}>
                  <SendIcon />
                </IconButton>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={onClose} sx={{ color: '#64748B', fontWeight: 600 }}>Close</Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteOpen} title="Delete Task" message="Are you sure you want to delete this task?" confirmText="Delete"
        onConfirm={handleDelete} onCancel={() => setDeleteOpen(false)} loading={deleteLoading} />

      <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar((p) => ({ ...p, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert onClose={() => setSnackbar((p) => ({ ...p, open: false }))} severity={snackbar.severity}
          variant="filled" sx={{ borderRadius: 3 }}>{snackbar.message}</Alert>
      </Snackbar>
    </>
  );
}