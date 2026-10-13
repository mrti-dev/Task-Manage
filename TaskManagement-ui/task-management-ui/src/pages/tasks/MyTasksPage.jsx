import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Box, Typography, Card, CardContent, Button, IconButton, TextField,
  Select, MenuItem, FormControl, InputLabel, Chip, Dialog, DialogTitle,
  DialogContent, DialogActions, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, TablePagination, Alert, Skeleton,
} from '@mui/material';
import {
  BookmarkBorderRounded as BookmarkIcon,
  BookmarkRounded as SavedBookmarkIcon,
  EditRounded as EditIcon,
  ArrowForwardRounded as ViewIcon,
} from '@mui/icons-material';
import { getTasksApi } from '../../api/taskApi';
import { getWorkspacesApi } from '../../api/workspaceApi';
import { getProjectsApi } from '../../api/projectApi';
import {
  getSavedFiltersApi, createSavedFilterApi, updateSavedFilterApi, deleteSavedFilterApi,
} from '../../api/savedFilterApi';
import { useAuth } from '../../contexts/AuthContext';
import TaskDetailModal from './TaskDetailModal';

export default function MyTasksPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // URL state
  const statusParam = searchParams.get('status') || '';
  const priorityParam = searchParams.get('priority') || '';
  const deadlineFromParam = searchParams.get('deadlineFrom') || '';
  const deadlineToParam = searchParams.get('deadlineTo') || '';
  const workspaceParam = searchParams.get('workspaceId') || '';
  const projectParam = searchParams.get('projectId') || '';
  const overdueParam = searchParams.get('overdue') === 'true';
  const pageParam = parseInt(searchParams.get('page') || '1', 10);
  const taskIdParam = searchParams.get('taskId');

  const [tasks, setTasks] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Dropdown options
  const [workspaces, setWorkspaces] = useState([]);
  const [projects, setProjects] = useState([]);

  // Saved filters state
  const [savedFilters, setSavedFilters] = useState([]);
  const [saveDialogOpen, setSaveDialogOpen] = useState(false);
  const [filterName, setFilterName] = useState('');
  const [editFilterDialog, setEditFilterDialog] = useState({ open: false, id: null, name: '' });

  // Task detail modal
  const selectedTaskId = taskIdParam ? Number(taskIdParam) : null;

  // Update URL params
  const updateQuery = (updates) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(updates).forEach(([key, val]) => {
      if (val === '' || val === null || val === false) {
        next.delete(key);
      } else {
        next.set(key, val);
      }
    });
    setSearchParams(next);
  };

  // Fetch workspaces
  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- tải workspace cho bộ lọc
    const fetchWs = async () => {
      try {
        const { data: res } = await getWorkspacesApi();
        setWorkspaces(res?.data ?? []);
      } catch (err) {
        console.warn('Failed to load workspaces:', err);
      }
    };
    fetchWs();
  }, []);

  // Fetch projects for selected workspace
  useEffect(() => {
    if (!workspaceParam) {
      // oxlint-disable-next-line react/set-state-in-effect -- xóa project khi bỏ chọn workspace
      setProjects([]);
      return;
    }
    const fetchPrj = async () => {
      try {
        const { data: res } = await getProjectsApi({ workspaceId: workspaceParam });
        setProjects(res?.data?.content ?? res?.data ?? []);
      } catch (err) {
        console.warn('Failed to load projects:', err);
      }
    };
    fetchPrj();
  }, [workspaceParam]);

  // Fetch saved filters
  const fetchSavedFilters = useCallback(async () => {
    try {
      const { data: res } = await getSavedFiltersApi();
      setSavedFilters(res?.data ?? []);
    } catch (err) {
      console.warn('Failed to load saved filters:', err);
    }
  }, []);

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- tải filter đã lưu khi mở trang
    fetchSavedFilters();
  }, [fetchSavedFilters]);

  // Fetch tasks
  const fetchTasks = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    setError('');
    try {
      const params = {
        assigneeId: user.id,
        page: pageParam,
        size: 15,
        ...(statusParam && { status: statusParam }),
        ...(priorityParam && { priority: priorityParam }),
        ...(deadlineFromParam && { deadlineFrom: deadlineFromParam }),
        ...(deadlineToParam && { deadlineTo: deadlineToParam }),
        ...(workspaceParam && { workspaceId: workspaceParam }),
        ...(projectParam && { projectId: projectParam }),
        ...(overdueParam && { overdue: true }),
      };
      const { data: res } = await getTasksApi(params);
      const data = res?.data ?? res;
      setTasks(data?.content ?? []);
      setTotalElements(data?.totalElements ?? 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể tải danh sách công việc');
    } finally {
      setLoading(false);
    }
  }, [user, statusParam, priorityParam, deadlineFromParam, deadlineToParam, workspaceParam, projectParam, overdueParam, pageParam]);

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- tải task theo URL filter
    fetchTasks();
  }, [fetchTasks]);

  // Handle Save Filter
  const handleSaveFilter = async () => {
    if (!filterName.trim()) return;
    try {
      const criteria = {
        status: statusParam,
        priority: priorityParam,
        deadlineFrom: deadlineFromParam,
        deadlineTo: deadlineToParam,
        workspaceId: workspaceParam,
        projectId: projectParam,
        overdue: overdueParam,
      };
      await createSavedFilterApi({
        name: filterName.trim(),
        criteriaJson: JSON.stringify(criteria),
        workspaceId: workspaceParam ? Number(workspaceParam) : null,
      });
      setFilterName('');
      setSaveDialogOpen(false);
      fetchSavedFilters();
    } catch (err) {
      console.error('Failed to save filter:', err);
    }
  };

  const handleApplyFilter = (criteriaJson) => {
    try {
      const criteria = JSON.parse(criteriaJson);
      updateQuery({
        status: criteria.status || '',
        priority: criteria.priority || '',
        deadlineFrom: criteria.deadlineFrom || '',
        deadlineTo: criteria.deadlineTo || '',
        workspaceId: criteria.workspaceId || '',
        projectId: criteria.projectId || '',
        overdue: criteria.overdue || false,
        page: 1,
      });
    } catch (err) {
      console.error('Failed to parse criteria:', err);
    }
  };

  const handleDeleteFilter = async (id) => {
    try {
      await deleteSavedFilterApi(id);
      fetchSavedFilters();
    } catch (err) {
      console.error('Failed to delete saved filter:', err);
    }
  };

  const handleRenameFilter = async () => {
    if (!editFilterDialog.name.trim()) return;
    try {
      await updateSavedFilterApi(editFilterDialog.id, { name: editFilterDialog.name.trim() });
      setEditFilterDialog({ open: false, id: null, name: '' });
      fetchSavedFilters();
    } catch (err) {
      console.error('Failed to rename filter:', err);
    }
  };

  const priorityColors = {
    LOW: { label: 'Low', color: '#94A3B8' },
    MEDIUM: { label: 'Medium', color: '#3B82F6' },
    HIGH: { label: 'High', color: '#F59E0B' },
    URGENT: { label: 'Urgent', color: '#EF4444' },
  };

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      {/* Title & Saved filters */}
      <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700} sx={{ color: '#17201F' }}>
            Nhiệm vụ của tôi (My Tasks)
          </Typography>
          <Typography variant="body2" sx={{ color: '#84918D' }}>
            Tất cả công việc được phân công cho bạn trên các workspace
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Button
            variant="outlined"
            size="small"
            startIcon={<BookmarkIcon />}
            onClick={() => setSaveDialogOpen(true)}
            sx={{ borderRadius: 2, textTransform: 'none', fontWeight: 600, color: '#0F766E', borderColor: '#0F766E' }}
          >
            Lưu bộ lọc hiện tại
          </Button>
        </Box>
      </Box>

      {/* Saved Filters Chips Bar */}
      {savedFilters.length > 0 && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2.5, flexWrap: 'wrap' }}>
          <Typography variant="caption" fontWeight={700} sx={{ color: '#84918D', mr: 0.5 }}>
            Bộ lọc đã lưu:
          </Typography>
          {savedFilters.map((f) => (
            <Box key={f.id} sx={{ display: 'inline-flex', alignItems: 'center' }}>
              <Chip
                label={f.name}
                onClick={() => handleApplyFilter(f.criteriaJson)}
                onDelete={() => handleDeleteFilter(f.id)}
                icon={<SavedBookmarkIcon sx={{ fontSize: '1rem !important', color: '#0F766E !important' }} />}
                sx={{
                  bgcolor: '#EEF3F0',
                  color: '#17201F',
                  fontWeight: 600,
                  '&:hover': { bgcolor: '#E2EBE6' },
                }}
              />
              <IconButton size="small" aria-label={`Đổi tên ${f.name}`} onClick={() => setEditFilterDialog({ open: true, id: f.id, name: f.name })}>
                <EditIcon fontSize="small" />
              </IconButton>
            </Box>
          ))}
        </Box>
      )}

      {/* Filter Toolbar */}
      <Card sx={{ borderRadius: 3, border: '1px solid #E2EBE6', boxShadow: 'none', mb: 3 }}>
        <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(2, minmax(0, 1fr))', md: 'repeat(4, minmax(0, 1fr))', xl: 'repeat(6, minmax(0, 1fr))' }, gap: 1.5 }}>
            {/* Workspace */}
            <Box>
              <FormControl size="small" fullWidth>
                <InputLabel>Workspace</InputLabel>
                <Select
                  value={workspaceParam}
                  label="Workspace"
                  onChange={(e) => updateQuery({ workspaceId: e.target.value, projectId: '', page: 1 })}
                >
                  <MenuItem value="">Tất cả Workspace</MenuItem>
                  {workspaces.map((w) => (
                    <MenuItem key={w.id} value={String(w.id)}>{w.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>

            {/* Project */}
            <Box>
              <FormControl size="small" fullWidth disabled={!workspaceParam}>
                <InputLabel>Dự án</InputLabel>
                <Select
                  value={projectParam}
                  label="Dự án"
                  onChange={(e) => updateQuery({ projectId: e.target.value, page: 1 })}
                >
                  <MenuItem value="">Tất cả dự án</MenuItem>
                  {projects.map((p) => (
                    <MenuItem key={p.id} value={String(p.id)}>{p.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>

            <TextField
              size="small"
              fullWidth
              type="date"
              label="Hạn chót từ"
              value={deadlineFromParam}
              slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: deadlineToParam || undefined } }}
              onChange={(e) => updateQuery({ deadlineFrom: e.target.value, page: 1 })}
            />

            <TextField
              size="small"
              fullWidth
              type="date"
              label="Hạn chót đến"
              value={deadlineToParam}
              slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: deadlineFromParam || undefined } }}
              onChange={(e) => updateQuery({ deadlineTo: e.target.value, page: 1 })}
            />

            {/* Status */}
            <Box>
              <FormControl size="small" fullWidth>
                <InputLabel>Trạng thái</InputLabel>
                <Select
                  value={statusParam}
                  label="Trạng thái"
                  onChange={(e) => updateQuery({ status: e.target.value, page: 1 })}
                >
                  <MenuItem value="">Tất cả</MenuItem>
                  <MenuItem value="TODO">To Do</MenuItem>
                  <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                  <MenuItem value="REVIEW">Review</MenuItem>
                  <MenuItem value="DONE">Done</MenuItem>
                </Select>
              </FormControl>
            </Box>

            {/* Priority */}
            <Box>
              <FormControl size="small" fullWidth>
                <InputLabel>Ưu tiên</InputLabel>
                <Select
                  value={priorityParam}
                  label="Ưu tiên"
                  onChange={(e) => updateQuery({ priority: e.target.value, page: 1 })}
                >
                  <MenuItem value="">Tất cả</MenuItem>
                  <MenuItem value="LOW">Low</MenuItem>
                  <MenuItem value="MEDIUM">Medium</MenuItem>
                  <MenuItem value="HIGH">High</MenuItem>
                  <MenuItem value="URGENT">Urgent</MenuItem>
                </Select>
              </FormControl>
            </Box>

            {/* Overdue toggle */}
            <Box sx={{ gridColumn: { xs: 'span 2', md: 'span 4', xl: 'span 6' }, display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
              <Button
                variant={overdueParam ? 'contained' : 'outlined'}
                size="small"
                onClick={() => updateQuery({ overdue: !overdueParam, page: 1 })}
                color={overdueParam ? 'error' : 'inherit'}
                sx={{
                  borderRadius: 2,
                  textTransform: 'none',
                  fontWeight: 600,
                  height: 40,
                }}
              >
                {overdueParam ? 'Chỉ task quá hạn (Bật)' : 'Lọc task quá hạn'}
              </Button>

              {(statusParam || priorityParam || deadlineFromParam || deadlineToParam || workspaceParam || projectParam || overdueParam) && (
                <Button
                  size="small"
                  onClick={() => setSearchParams(new URLSearchParams())}
                  sx={{ color: '#84918D', textTransform: 'none', fontWeight: 600 }}
                >
                  Đặt lại
                </Button>
              )}
            </Box>
          </Box>
        </CardContent>
      </Card>

      {error && <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>{error}</Alert>}

      {/* Tasks Table */}
      <Card sx={{ borderRadius: 3, border: '1px solid #E2EBE6', boxShadow: 'none' }}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow sx={{ bgcolor: '#F3F6F4' }}>
                <TableCell sx={{ fontWeight: 700 }}>Tiêu đề nhiệm vụ</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Dự án</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Ưu tiên</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Trạng thái</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Hạn chót</TableCell>
                <TableCell align="right" sx={{ fontWeight: 700 }}>Thao tác</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton width="60%" /></TableCell>
                    <TableCell><Skeleton width={100} /></TableCell>
                    <TableCell><Skeleton width={60} /></TableCell>
                    <TableCell><Skeleton width={80} /></TableCell>
                    <TableCell><Skeleton width={90} /></TableCell>
                    <TableCell align="right"><Skeleton width={40} /></TableCell>
                  </TableRow>
                ))
              ) : tasks.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 6, color: '#84918D' }}>
                    <Typography variant="body1" fontWeight={600} sx={{ mb: 0.5 }}>Không tìm thấy nhiệm vụ nào</Typography>
                    <Typography variant="caption">Hãy thay đổi bộ lọc hoặc kiểm tra lại workspace</Typography>
                  </TableCell>
                </TableRow>
              ) : (
                tasks.map((t) => {
                  const pInfo = priorityColors[t.priority] || { label: t.priority, color: '#64748B' };
                  return (
                    <TableRow
                      key={t.id}
                      hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => updateQuery({ taskId: t.id })}
                    >
                      <TableCell>
                        <Typography variant="body2" fontWeight={600} sx={{ color: '#17201F' }}>
                          {t.title}
                        </Typography>
                        {t.subtaskCount > 0 && (
                          <Typography variant="caption" sx={{ color: '#84918D' }}>
                            Subtasks: {t.completedSubtaskCount}/{t.subtaskCount}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" sx={{ color: '#42504C' }}>
                          {t.projectName}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={pInfo.label}
                          sx={{
                            fontWeight: 700,
                            bgcolor: `${pInfo.color}15`,
                            color: pInfo.color,
                            border: `1px solid ${pInfo.color}40`,
                          }}
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={t.status}
                          sx={{
                            fontWeight: 600,
                            bgcolor: t.status === 'DONE' ? '#E6F4EA' : '#EEF3F0',
                            color: t.status === 'DONE' ? '#10B981' : '#42504C',
                          }}
                        />
                      </TableCell>
                      <TableCell>
                        {t.deadline ? (
                          <Chip
                            size="small"
                            label={t.deadline}
                            sx={{
                              fontWeight: 600,
                              bgcolor: t.overdue ? '#FEE2E2' : t.dueSoon ? '#FEF3C7' : '#EEF3F0',
                              color: t.overdue ? '#EF4444' : t.dueSoon ? '#D97706' : '#42504C',
                            }}
                          />
                        ) : (
                          <Typography variant="caption" sx={{ color: '#CBD5D1' }}>Không có</Typography>
                        )}
                      </TableCell>
                      <TableCell align="right">
                        <IconButton size="small" sx={{ color: '#0F766E' }}>
                          <ViewIcon fontSize="small" />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <TablePagination
          component="div"
          count={totalElements}
          page={pageParam - 1}
          onPageChange={(_, next) => updateQuery({ page: next + 1 })}
          rowsPerPage={15}
          rowsPerPageOptions={[15]}
        />
      </Card>

      {/* Dialog Save Filter */}
      <Dialog open={saveDialogOpen} onClose={() => setSaveDialogOpen(false)} maxWidth="xs" fullWidth PaperProps={{ sx: { borderRadius: 3 } }}>
        <DialogTitle sx={{ fontWeight: 700 }}>Lưu bộ lọc cá nhân</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            size="small"
            label="Tên bộ lọc"
            placeholder="Ví dụ: Task gấp trong tháng"
            value={filterName}
            onChange={(e) => setFilterName(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setSaveDialogOpen(false)} sx={{ color: '#84918D' }}>Hủy</Button>
          <Button
            variant="contained"
            onClick={handleSaveFilter}
            disabled={!filterName.trim()}
            sx={{ bgcolor: '#0F766E', '&:hover': { bgcolor: '#0D655E' } }}
          >
            Lưu bộ lọc
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={editFilterDialog.open} onClose={() => setEditFilterDialog({ open: false, id: null, name: '' })} maxWidth="xs" fullWidth>
        <DialogTitle>Đổi tên bộ lọc</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth size="small" label="Tên bộ lọc" value={editFilterDialog.name}
            onChange={(event) => setEditFilterDialog((state) => ({ ...state, name: event.target.value }))} sx={{ mt: 1 }} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditFilterDialog({ open: false, id: null, name: '' })}>Hủy</Button>
          <Button variant="contained" onClick={handleRenameFilter} disabled={!editFilterDialog.name.trim()}>Lưu</Button>
        </DialogActions>
      </Dialog>

      {/* Task detail modal */}
      {selectedTaskId && (
        <TaskDetailModal
          taskId={selectedTaskId}
          open={Boolean(selectedTaskId)}
          onClose={() => {
            updateQuery({ taskId: null });
            fetchTasks();
          }}
          onUpdate={() => fetchTasks()}
          onDelete={() => {
            updateQuery({ taskId: null });
            fetchTasks();
          }}
        />
      )}
    </Box>
  );
}
