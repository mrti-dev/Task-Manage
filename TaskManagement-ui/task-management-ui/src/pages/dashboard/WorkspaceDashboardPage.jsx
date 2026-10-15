import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Typography, Card, CardContent, Select, MenuItem, FormControl, InputLabel,
  TextField, Button, LinearProgress, Chip, Avatar, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Skeleton, Alert,
} from '@mui/material';
import {
  AssignmentRounded as TaskIcon,
  CheckCircleRounded as CompletedIcon,
  WarningRounded as OverdueIcon,
  ScheduleRounded as DueSoonIcon,
  TrendingUpRounded as RateIcon,
  RefreshRounded as RefreshIcon,
  PeopleRounded as PeopleIcon,
  FolderRounded as ProjectIcon,
} from '@mui/icons-material';
import { getDashboardApi } from '../../api/dashboardApi';
import { getProjectsApi } from '../../api/projectApi';
import { getWorkspacesApi } from '../../api/workspaceApi';

export default function WorkspaceDashboardPage() {
  const { id: workspaceIdParam } = useParams();
  const navigate = useNavigate();
  const [workspaces, setWorkspaces] = useState([]);
  const [selectedWorkspace, setSelectedWorkspace] = useState(workspaceIdParam ? Number(workspaceIdParam) : '');
  const [dashboard, setDashboard] = useState(null);
  const [projects, setProjects] = useState([]);
  const [selectedProject, setSelectedProject] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (workspaceIdParam) {
      // oxlint-disable-next-line react/set-state-in-effect -- đồng bộ workspace từ route param
      setSelectedWorkspace(Number(workspaceIdParam));
    }
  }, [workspaceIdParam]);

  useEffect(() => {
    const fetchWs = async () => {
      try {
        const { data: res } = await getWorkspacesApi();
        const list = res?.data ?? res ?? [];
        setWorkspaces(list);
        if (!selectedWorkspace && list.length > 0) {
          const stored = Number(localStorage.getItem('lastWorkspaceId'));
          const defaultWs = list.some((w) => w.id === stored) ? stored : list[0].id;
          setSelectedWorkspace(defaultWs);
        }
      } catch (err) {
        console.warn('Failed to load workspaces:', err);
      }
    };
    fetchWs();
  }, [selectedWorkspace]);

  useEffect(() => {
    const fetchPrj = async () => {
      if (!selectedWorkspace) {
        setProjects([]);
        return;
      }
      localStorage.setItem('lastWorkspaceId', String(selectedWorkspace));
      try {
        const { data: res } = await getProjectsApi({ workspaceId: selectedWorkspace });
        setProjects(res?.data?.content ?? res?.data ?? []);
      } catch (err) {
        console.warn('Failed to load projects:', err);
      }
    };
    fetchPrj();
  }, [selectedWorkspace]);

  const fetchDashboard = useCallback(async () => {
    if (!selectedWorkspace) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const params = {
        ...(selectedProject && { projectId: selectedProject }),
        ...(dateFrom && { from: dateFrom }),
        ...(dateTo && { to: dateTo }),
      };
      const { data: res } = await getDashboardApi(selectedWorkspace, params);
      setDashboard(res?.data ?? null);
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể tải dữ liệu dashboard.');
    } finally {
      setLoading(false);
    }
  }, [selectedWorkspace, selectedProject, dateFrom, dateTo]);

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- tải dashboard theo workspace và filter hiện tại
    fetchDashboard();
  }, [fetchDashboard]);

  const handleWorkspaceChange = (newWsId) => {
    setSelectedWorkspace(newWsId);
    setSelectedProject('');
    navigate(`/workspaces/${newWsId}/dashboard`, { replace: true });
  };

  const statusColors = {
    TODO: { label: 'To Do', color: '#64748B' },
    IN_PROGRESS: { label: 'In Progress', color: '#3B82F6' },
    REVIEW: { label: 'Review', color: '#8B5CF6' },
    DONE: { label: 'Done', color: '#10B981' },
  };

  const priorityColors = {
    LOW: { label: 'Low', color: '#94A3B8' },
    MEDIUM: { label: 'Medium', color: '#3B82F6' },
    HIGH: { label: 'High', color: '#F59E0B' },
    URGENT: { label: 'Urgent', color: '#EF4444' },
  };

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      {/* Header & Filters */}
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'stretch', gap: 2, mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700} sx={{ color: '#17201F' }}>
            Dashboard Tiến độ & Workload
          </Typography>
          <Typography variant="body2" sx={{ color: '#84918D' }}>
            Tổng quan hiệu suất công việc và phân bổ thành viên trong workspace
          </Typography>
        </Box>

        {/* Filter bar */}
        <Box sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: '1fr',
            sm: 'repeat(2, minmax(0, 1fr))',
            lg: 'minmax(200px, 1.2fr) minmax(200px, 1.2fr) repeat(2, minmax(160px, 1fr)) auto',
          },
          alignItems: 'end',
          gap: 1.5,
          p: 1.5,
          bgcolor: '#F7FAF8',
          border: '1px solid #DCE4E0',
          borderRadius: 2,
        }}>
          <FormControl size="small" sx={{ width: '100%' }}>
            <InputLabel>Workspace</InputLabel>
            <Select
              value={selectedWorkspace || ''}
              label="Workspace"
              onChange={(e) => handleWorkspaceChange(e.target.value)}
            >
              {workspaces.map((w) => (
                <MenuItem key={w.id} value={w.id}>{w.name}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ width: '100%' }}>
            <InputLabel>Dự án</InputLabel>
            <Select
              value={selectedProject}
              label="Dự án"
              onChange={(e) => setSelectedProject(e.target.value)}
            >
              <MenuItem value="">Tất cả dự án</MenuItem>
              {projects.map((p) => (
                <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, minWidth: 0 }}>
            <Typography variant="caption" sx={{ color: '#66736F', fontWeight: 600 }}>Từ ngày</Typography>
            <TextField
              size="small"
              type="date"
              value={dateFrom}
              onChange={(e) => setDateFrom(e.target.value)}
              inputProps={{ 'aria-label': 'Từ ngày' }}
              fullWidth
            />
          </Box>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, minWidth: 0 }}>
            <Typography variant="caption" sx={{ color: '#66736F', fontWeight: 600 }}>Đến ngày</Typography>
            <TextField
              size="small"
              type="date"
              value={dateTo}
              onChange={(e) => setDateTo(e.target.value)}
              inputProps={{ 'aria-label': 'Đến ngày' }}
              fullWidth
            />
          </Box>

          <Button
            variant="outlined"
            size="medium"
            onClick={fetchDashboard}
            startIcon={<RefreshIcon />}
            sx={{ minHeight: 40, borderRadius: 2, textTransform: 'none', fontWeight: 600, color: '#0F766E', borderColor: '#0F766E' }}
          >
            Làm mới
          </Button>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>{error}</Alert>}

      {/* Top Metric Cards */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', xl: 'repeat(5, minmax(0, 1fr))' }, gap: 2.5, mb: 3 }}>
        {/* Total Tasks */}
        <Box>
          <Card sx={{ borderRadius: 3, boxShadow: '0 2px 12px rgba(0,0,0,0.04)', border: '1px solid #E2EBE6' }}>
            <CardContent sx={{ p: 2.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                <Typography variant="body2" fontWeight={600} sx={{ color: '#84918D' }}>Tổng task</Typography>
                <Avatar sx={{ width: 36, height: 36, bgcolor: '#EEF3F0', color: '#0F766E' }}><TaskIcon fontSize="small" /></Avatar>
              </Box>
              {loading ? <Skeleton variant="text" width={60} height={40} /> : (
                <Typography variant="h4" fontWeight={800} sx={{ color: '#17201F' }}>
                  {dashboard?.totalTasks ?? 0}
                </Typography>
              )}
            </CardContent>
          </Card>
        </Box>

        {/* Completed Tasks */}
        <Box>
          <Card sx={{ borderRadius: 3, boxShadow: '0 2px 12px rgba(0,0,0,0.04)', border: '1px solid #E2EBE6' }}>
            <CardContent sx={{ p: 2.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                <Typography variant="body2" fontWeight={600} sx={{ color: '#84918D' }}>Đã hoàn thành</Typography>
                <Avatar sx={{ width: 36, height: 36, bgcolor: '#E6F4EA', color: '#10B981' }}><CompletedIcon fontSize="small" /></Avatar>
              </Box>
              {loading ? <Skeleton variant="text" width={60} height={40} /> : (
                <Typography variant="h4" fontWeight={800} sx={{ color: '#10B981' }}>
                  {dashboard?.completedTasks ?? 0}
                </Typography>
              )}
            </CardContent>
          </Card>
        </Box>

        {/* Completion Rate */}
        <Box>
          <Card sx={{ borderRadius: 3, boxShadow: '0 2px 12px rgba(0,0,0,0.04)', border: '1px solid #E2EBE6' }}>
            <CardContent sx={{ p: 2.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                <Typography variant="body2" fontWeight={600} sx={{ color: '#84918D' }}>Tỷ lệ hoàn thành</Typography>
                <Avatar sx={{ width: 36, height: 36, bgcolor: '#E0F2FE', color: '#0284C7' }}><RateIcon fontSize="small" /></Avatar>
              </Box>
              {loading ? <Skeleton variant="text" width={60} height={40} /> : (
                <Typography variant="h4" fontWeight={800} sx={{ color: '#0284C7' }}>
                  {dashboard?.completionRate ?? 0}%
                </Typography>
              )}
            </CardContent>
          </Card>
        </Box>

        {/* Overdue Tasks */}
        <Box>
          <Card sx={{ borderRadius: 3, boxShadow: '0 2px 12px rgba(0,0,0,0.04)', border: '1px solid #E2EBE6', bgcolor: dashboard?.overdueTasks > 0 ? '#FEF2F2' : '#fff' }}>
            <CardContent sx={{ p: 2.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                <Typography variant="body2" fontWeight={600} sx={{ color: dashboard?.overdueTasks > 0 ? '#DC2626' : '#84918D' }}>Quá hạn</Typography>
                <Avatar sx={{ width: 36, height: 36, bgcolor: '#FEE2E2', color: '#EF4444' }}><OverdueIcon fontSize="small" /></Avatar>
              </Box>
              {loading ? <Skeleton variant="text" width={60} height={40} /> : (
                <Typography variant="h4" fontWeight={800} sx={{ color: '#EF4444' }}>
                  {dashboard?.overdueTasks ?? 0}
                </Typography>
              )}
            </CardContent>
          </Card>
        </Box>

        {/* Due Soon Tasks */}
        <Box>
          <Card sx={{ borderRadius: 3, boxShadow: '0 2px 12px rgba(0,0,0,0.04)', border: '1px solid #E2EBE6' }}>
            <CardContent sx={{ p: 2.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                <Typography variant="body2" fontWeight={600} sx={{ color: '#84918D' }}>Sắp đến hạn</Typography>
                <Avatar sx={{ width: 36, height: 36, bgcolor: '#FEF3C7', color: '#D97706' }}><DueSoonIcon fontSize="small" /></Avatar>
              </Box>
              {loading ? <Skeleton variant="text" width={60} height={40} /> : (
                <Typography variant="h4" fontWeight={800} sx={{ color: '#D97706' }}>
                  {dashboard?.dueSoonTasks ?? 0}
                </Typography>
              )}
            </CardContent>
          </Card>
        </Box>
      </Box>

      {/* Charts Row: Status & Priority */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' }, gap: 2.5, mb: 3 }}>
        {/* Status Distribution */}
        <Box>
          <Card sx={{ borderRadius: 3, border: '1px solid #E2EBE6', boxShadow: 'none', height: '100%' }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="subtitle1" fontWeight={700} sx={{ color: '#17201F', mb: 2 }}>
                Phân bố theo trạng thái
              </Typography>
              {loading ? <Skeleton variant="rectangular" height={160} sx={{ borderRadius: 2 }} /> : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  {Object.entries(statusColors).map(([statusKey, info]) => {
                    const count = dashboard?.tasksByStatus?.[statusKey] ?? 0;
                    const percent = dashboard?.totalTasks > 0 ? Math.round((count / dashboard.totalTasks) * 100) : 0;
                    return (
                      <Box key={statusKey}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                          <Typography variant="body2" fontWeight={600} sx={{ color: '#42504C' }}>
                            {info.label} ({count})
                          </Typography>
                          <Typography variant="body2" fontWeight={700} sx={{ color: info.color }}>
                            {percent}%
                          </Typography>
                        </Box>
                        <LinearProgress
                          variant="determinate"
                          value={percent}
                          sx={{
                            height: 8, borderRadius: 4,
                            bgcolor: '#EEF3F0',
                            '& .MuiLinearProgress-bar': { bgcolor: info.color, borderRadius: 4 },
                          }}
                        />
                      </Box>
                    );
                  })}
                </Box>
              )}
            </CardContent>
          </Card>
        </Box>

        {/* Priority Distribution */}
        <Box>
          <Card sx={{ borderRadius: 3, border: '1px solid #E2EBE6', boxShadow: 'none', height: '100%' }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="subtitle1" fontWeight={700} sx={{ color: '#17201F', mb: 2 }}>
                Phân bố theo mức độ ưu tiên
              </Typography>
              {loading ? <Skeleton variant="rectangular" height={160} sx={{ borderRadius: 2 }} /> : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  {Object.entries(priorityColors).map(([priorityKey, info]) => {
                    const count = dashboard?.tasksByPriority?.[priorityKey] ?? 0;
                    const percent = dashboard?.totalTasks > 0 ? Math.round((count / dashboard.totalTasks) * 100) : 0;
                    return (
                      <Box key={priorityKey}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                          <Typography variant="body2" fontWeight={600} sx={{ color: '#42504C' }}>
                            {info.label} ({count})
                          </Typography>
                          <Typography variant="body2" fontWeight={700} sx={{ color: info.color }}>
                            {percent}%
                          </Typography>
                        </Box>
                        <LinearProgress
                          variant="determinate"
                          value={percent}
                          sx={{
                            height: 8, borderRadius: 4,
                            bgcolor: '#EEF3F0',
                            '& .MuiLinearProgress-bar': { bgcolor: info.color, borderRadius: 4 },
                          }}
                        />
                      </Box>
                    );
                  })}
                </Box>
              )}
            </CardContent>
          </Card>
        </Box>
      </Box>

      {/* Member Workloads Table */}
      <Card sx={{ borderRadius: 3, border: '1px solid #E2EBE6', boxShadow: 'none', mb: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <PeopleIcon sx={{ color: '#0F766E' }} />
            <Typography variant="subtitle1" fontWeight={700} sx={{ color: '#17201F' }}>
              Workload thành viên
            </Typography>
          </Box>
          <TableContainer>
            <Table size="medium">
              <TableHead>
                <TableRow sx={{ bgcolor: '#F3F6F4' }}>
                  <TableCell sx={{ fontWeight: 700 }}>Thành viên</TableCell>
                  <TableCell align="center" sx={{ fontWeight: 700 }}>Đang thực hiện</TableCell>
                  <TableCell align="center" sx={{ fontWeight: 700 }}>Quá hạn</TableCell>
                  <TableCell align="center" sx={{ fontWeight: 700 }}>Hoàn thành trong kỳ</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Theo mức độ ưu tiên</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  Array.from({ length: 3 }).map((_, i) => (
                    <TableRow key={i}>
                      <TableCell><Skeleton width={120} /></TableCell>
                      <TableCell><Skeleton width={40} /></TableCell>
                      <TableCell><Skeleton width={40} /></TableCell>
                      <TableCell><Skeleton width={40} /></TableCell>
                      <TableCell><Skeleton width={180} /></TableCell>
                    </TableRow>
                  ))
                ) : !dashboard?.memberWorkloads?.length ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 3, color: '#84918D' }}>
                      Chưa có thành viên nào được gán task
                    </TableCell>
                  </TableRow>
                ) : (
                  dashboard.memberWorkloads.map((m) => (
                    <TableRow key={m.userId} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                          <Avatar src={m.avatar || ''} sx={{ width: 32, height: 32, bgcolor: '#0F766E', fontSize: '1.0625rem' }}>
                            {m.username?.[0]?.toUpperCase()}
                          </Avatar>
                          <Box>
                            <Typography variant="body2" fontWeight={600} sx={{ color: '#17201F' }}>
                              {m.username}
                            </Typography>
                            <Typography variant="caption" sx={{ color: '#84918D' }}>
                              {m.email}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell align="center">
                        <Chip size="small" label={m.activeTasks} sx={{ fontWeight: 700, bgcolor: '#EEF3F0', color: '#17201F' }} />
                      </TableCell>
                      <TableCell align="center">
                        <Chip
                          size="small"
                          label={m.overdueTasks}
                          sx={{
                            fontWeight: 700,
                            bgcolor: m.overdueTasks > 0 ? '#FEE2E2' : '#EEF3F0',
                            color: m.overdueTasks > 0 ? '#EF4444' : '#84918D',
                          }}
                        />
                      </TableCell>
                      <TableCell align="center">
                        <Chip size="small" label={m.completedInRange} sx={{ fontWeight: 700, bgcolor: '#E6F4EA', color: '#10B981' }} />
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                          {Object.entries(m.tasksByPriority || {}).map(([pKey, pCount]) => {
                            if (pCount === 0) return null;
                            const pInfo = priorityColors[pKey] || { label: pKey, color: '#64748B' };
                            return (
                              <Chip
                                key={pKey}
                                size="small"
                                label={`${pInfo.label}: ${pCount}`}
                                sx={{
                                  fontSize: '1rem',
                                  fontWeight: 600,
                                  bgcolor: `${pInfo.color}15`,
                                  color: pInfo.color,
                                  border: `1px solid ${pInfo.color}40`,
                                }}
                              />
                            );
                          })}
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Projects Progress Table */}
      <Card sx={{ borderRadius: 3, border: '1px solid #E2EBE6', boxShadow: 'none', mb: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <ProjectIcon sx={{ color: '#0F766E' }} />
            <Typography variant="subtitle1" fontWeight={700} sx={{ color: '#17201F' }}>
              Tiến độ theo Dự án
            </Typography>
          </Box>
          <TableContainer>
            <Table size="medium">
              <TableHead>
                <TableRow sx={{ bgcolor: '#F3F6F4' }}>
                  <TableCell sx={{ fontWeight: 700 }}>Dự án</TableCell>
                  <TableCell align="center" sx={{ fontWeight: 700 }}>Tổng task</TableCell>
                  <TableCell align="center" sx={{ fontWeight: 700 }}>Đã xong</TableCell>
                  <TableCell align="center" sx={{ fontWeight: 700 }}>Quá hạn</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Tiến độ</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  Array.from({ length: 2 }).map((_, i) => (
                    <TableRow key={i}>
                      <TableCell><Skeleton width={140} /></TableCell>
                      <TableCell><Skeleton width={40} /></TableCell>
                      <TableCell><Skeleton width={40} /></TableCell>
                      <TableCell><Skeleton width={40} /></TableCell>
                      <TableCell><Skeleton width={180} /></TableCell>
                    </TableRow>
                  ))
                ) : !dashboard?.projectProgress?.length ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 3, color: '#84918D' }}>
                      Chưa có dự án nào
                    </TableCell>
                  </TableRow>
                ) : (
                  dashboard.projectProgress.map((p) => (
                    <TableRow
                      key={p.projectId}
                      hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => navigate(`/workspaces/${selectedWorkspace}/projects/${p.projectId}`)}
                    >
                      <TableCell>
                        <Typography variant="body2" fontWeight={700} sx={{ color: '#0F766E' }}>
                          {p.projectName}
                        </Typography>
                      </TableCell>
                      <TableCell align="center">
                        <Typography variant="body2" fontWeight={600}>{p.totalTasks}</Typography>
                      </TableCell>
                      <TableCell align="center">
                        <Typography variant="body2" fontWeight={600} sx={{ color: '#10B981' }}>{p.completedTasks}</Typography>
                      </TableCell>
                      <TableCell align="center">
                        <Typography variant="body2" fontWeight={600} sx={{ color: p.overdueTasks > 0 ? '#EF4444' : '#84918D' }}>
                          {p.overdueTasks}
                        </Typography>
                      </TableCell>
                      <TableCell sx={{ width: '35%' }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <Box sx={{ flex: 1 }}>
                            <LinearProgress
                              variant="determinate"
                              value={p.completionRate}
                              sx={{
                                height: 8, borderRadius: 4,
                                bgcolor: '#EEF3F0',
                                '& .MuiLinearProgress-bar': { bgcolor: '#0F766E', borderRadius: 4 },
                              }}
                            />
                          </Box>
                          <Typography variant="caption" fontWeight={700} sx={{ minWidth: 40, color: '#17201F' }}>
                            {p.completionRate}%
                          </Typography>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Overdue Trend */}
      {dashboard?.overdueTrends && dashboard.overdueTrends.length > 0 && (
        <Card sx={{ borderRadius: 3, border: '1px solid #E2EBE6', boxShadow: 'none' }}>
          <CardContent sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
              <OverdueIcon sx={{ color: '#EF4444' }} />
              <Typography variant="subtitle1" fontWeight={700} sx={{ color: '#17201F' }}>
                Biểu đồ Task quá hạn theo ngày
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', gap: 2, overflowX: 'auto', py: 2 }}>
              {dashboard.overdueTrends.map((trend) => (
                <Box
                  key={trend.date}
                  sx={{
                    minWidth: 100,
                    p: 1.5,
                    borderRadius: 2,
                    bgcolor: '#FEF2F2',
                    border: '1px solid #FEE2E2',
                    textAlign: 'center',
                  }}
                >
                  <Typography variant="caption" sx={{ color: '#84918D', display: 'block', mb: 0.5 }}>
                    {trend.date}
                  </Typography>
                  <Typography variant="h6" fontWeight={800} sx={{ color: '#EF4444' }}>
                    {trend.count}
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#EF4444' }}>task quá hạn</Typography>
                </Box>
              ))}
            </Box>
          </CardContent>
        </Card>
      )}
    </Box>
  );
}
