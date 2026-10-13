import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  Box, Typography, Button, IconButton, Select, MenuItem, FormControl, InputLabel,
  Card, Chip, Alert, ButtonGroup,
} from '@mui/material';
import {
  ChevronLeftRounded as PrevIcon,
  ChevronRightRounded as NextIcon,
  CalendarMonthRounded as MonthIcon,
  ViewWeekRounded as WeekIcon,
} from '@mui/icons-material';
import { getCalendarTasksApi } from '../../api/taskApi';
import { getWorkspacesApi } from '../../api/workspaceApi';
import { getProjectsApi } from '../../api/projectApi';
import TaskDetailModal from '../tasks/TaskDetailModal';

const DAYS_OF_WEEK = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const toDateKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

export default function CalendarPage() {
  const { id: workspaceIdParam } = useParams();
  const [currentDate, setCurrentDate] = useState(() => new Date());
  const [todayStr] = useState(() => toDateKey(new Date()));
  const [viewMode, setViewMode] = useState('month'); // 'month' or 'week'
  const [tasks, setTasks] = useState([]);
  const [_loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Filters
  const [workspaces, setWorkspaces] = useState([]);
  const [selectedWorkspace, setSelectedWorkspace] = useState(workspaceIdParam ? Number(workspaceIdParam) : '');
  const [projects, setProjects] = useState([]);
  const [selectedProject, setSelectedProject] = useState('');
  const [selectedPriority, setSelectedPriority] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');

  // Task detail modal
  const [selectedTaskId, setSelectedTaskId] = useState(null);

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- đồng bộ workspace mặc định từ API
    const fetchWs = async () => {
      try {
        const { data: res } = await getWorkspacesApi();
        const list = res?.data ?? res ?? [];
        setWorkspaces(list);
        if (!selectedWorkspace && list.length > 0) {
          setSelectedWorkspace(list[0].id);
        }
      } catch (err) {
        console.warn('Failed to load workspaces:', err);
      }
    };
    fetchWs();
  }, [selectedWorkspace]);

  useEffect(() => {
    if (!selectedWorkspace) return;
    // oxlint-disable-next-line react/set-state-in-effect -- tải dự án theo workspace đang chọn
    const fetchPrj = async () => {
      try {
        const { data: res } = await getProjectsApi({ workspaceId: selectedWorkspace });
        setProjects(res?.data?.content ?? res?.data ?? []);
      } catch (err) {
        console.warn('Failed to load projects:', err);
      }
    };
    fetchPrj();
  }, [selectedWorkspace]);

  // Calculate range based on viewMode & currentDate
  const getDateRange = useCallback(() => {
    const y = currentDate.getFullYear();
    const m = currentDate.getMonth();

    if (viewMode === 'month') {
      const firstDayOfMonth = new Date(y, m, 1);
      // Determine start date (Monday of the first week)
      let dayOfWeek = firstDayOfMonth.getDay();
      dayOfWeek = dayOfWeek === 0 ? 6 : dayOfWeek - 1; // convert 0(Sun) to 6, 1(Mon) to 0
      const startDate = new Date(y, m, 1 - dayOfWeek);

      // End date: 42 days (6 weeks) from start date to make grid full
      const endDate = new Date(startDate);
      endDate.setDate(startDate.getDate() + 41);

      return {
        from: toDateKey(startDate),
        to: toDateKey(endDate),
        startDate,
        endDate,
      };
    } else {
      // Week view: Monday to Sunday of current week
      let dayOfWeek = currentDate.getDay();
      dayOfWeek = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
      const startDate = new Date(currentDate);
      startDate.setDate(currentDate.getDate() - dayOfWeek);
      const endDate = new Date(startDate);
      endDate.setDate(startDate.getDate() + 6);

      return {
        from: toDateKey(startDate),
        to: toDateKey(endDate),
        startDate,
        endDate,
      };
    }
  }, [currentDate, viewMode]);

  const fetchTasks = useCallback(async () => {
    setLoading(true);
    setError('');
    const { from, to } = getDateRange();
    try {
      const params = {
        from,
        to,
        ...(selectedWorkspace && { workspaceId: selectedWorkspace }),
        ...(selectedProject && { projectId: selectedProject }),
        ...(selectedPriority && { priority: selectedPriority }),
        ...(selectedStatus && { status: selectedStatus }),
      };
      const { data: res } = await getCalendarTasksApi(params);
      setTasks(res?.data ?? []);
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể tải lịch công việc');
    } finally {
      setLoading(false);
    }
  }, [getDateRange, selectedWorkspace, selectedProject, selectedPriority, selectedStatus]);

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- tải task theo ngày và bộ lọc hiện tại
    fetchTasks();
  }, [fetchTasks]);

  // Navigation handlers
  const handlePrev = () => {
    setCurrentDate((prev) => {
      const next = new Date(prev);
      if (viewMode === 'month') {
        next.setMonth(next.getMonth() - 1);
      } else {
        next.setDate(next.getDate() - 7);
      }
      return next;
    });
  };

  const handleNext = () => {
    setCurrentDate((prev) => {
      const next = new Date(prev);
      if (viewMode === 'month') {
        next.setMonth(next.getMonth() + 1);
      } else {
        next.setDate(next.getDate() + 7);
      }
      return next;
    });
  };

  // Helper: map tasks by date string (YYYY-MM-DD)
  const tasksByDate = tasks.reduce((acc, task) => {
    if (task.deadline) {
      acc[task.deadline] = acc[task.deadline] || [];
      acc[task.deadline].push(task);
    }
    return acc;
  }, {});

  // Generate grid cells
  const { startDate } = getDateRange();
  const dayCount = viewMode === 'month' ? 42 : 7;
  const days = [];
  for (let i = 0; i < dayCount; i++) {
    const d = new Date(startDate);
    d.setDate(startDate.getDate() + i);
    days.push(d);
  }

  const currentMonth = currentDate.getMonth();

  const getTaskColor = (task) => {
    if (task.status === 'DONE') return { bg: '#E6F4EA', border: '#34A853', text: '#137333' };
    if (task.overdue) return { bg: '#FCE8E6', border: '#EA4335', text: '#C5221F' };
    if (task.dueSoon) return { bg: '#FEF7E0', border: '#FBBC04', text: '#B06000' };
    return { bg: '#E8F0FE', border: '#4285F4', text: '#1967D2' };
  };

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      {/* Header controls */}
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 1.5 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: { xs: 1, sm: 2 } }}>
            <Typography variant="h5" fontWeight={700} sx={{ color: '#17201F', fontVariantNumeric: 'tabular-nums' }}>
              {viewMode === 'month'
                ? currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
                : `${days[0]?.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} – ${days[6]?.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}`}
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <IconButton aria-label="Khoảng thời gian trước" onClick={handlePrev} size="small" sx={{ width: 36, height: 36 }}>
                <PrevIcon />
              </IconButton>
              <IconButton aria-label="Khoảng thời gian tiếp theo" onClick={handleNext} size="small" sx={{ width: 36, height: 36 }}>
                <NextIcon />
              </IconButton>
            </Box>
          </Box>

        </Box>

        {/* View Switcher & Filters */}
        <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'flex-start', gap: 1.5 }}>
          <ButtonGroup size="small" variant="contained">
            <Button
              onClick={() => setViewMode('month')}
              sx={{
                bgcolor: viewMode === 'month' ? '#0F766E' : '#EEF3F0',
                color: viewMode === 'month' ? '#fff' : '#42504C',
                fontWeight: 600,
                textTransform: 'none',
                '&:hover': { bgcolor: viewMode === 'month' ? '#0D655E' : '#E2EBE6' },
              }}
              startIcon={<MonthIcon />}
            >
              Tháng
            </Button>
            <Button
              onClick={() => setViewMode('week')}
              sx={{
                bgcolor: viewMode === 'week' ? '#0F766E' : '#EEF3F0',
                color: viewMode === 'week' ? '#fff' : '#42504C',
                fontWeight: 600,
                textTransform: 'none',
                '&:hover': { bgcolor: viewMode === 'week' ? '#0D655E' : '#E2EBE6' },
              }}
              startIcon={<WeekIcon />}
            >
              Tuần
            </Button>
          </ButtonGroup>

          {/* Workspace select */}
          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>Workspace</InputLabel>
            <Select
              value={selectedWorkspace}
              label="Workspace"
              onChange={(e) => {
                setSelectedWorkspace(e.target.value);
                setSelectedProject('');
              }}
            >
              {workspaces.map((w) => (
                <MenuItem key={w.id} value={w.id}>{w.name}</MenuItem>
              ))}
            </Select>
          </FormControl>

          {/* Project filter */}
          <FormControl size="small" sx={{ minWidth: 140 }}>
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

          {/* Priority filter */}
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>Ưu tiên</InputLabel>
            <Select
              value={selectedPriority}
              label="Ưu tiên"
              onChange={(e) => setSelectedPriority(e.target.value)}
            >
              <MenuItem value="">Tất cả</MenuItem>
              <MenuItem value="LOW">Low</MenuItem>
              <MenuItem value="MEDIUM">Medium</MenuItem>
              <MenuItem value="HIGH">High</MenuItem>
              <MenuItem value="URGENT">Urgent</MenuItem>
            </Select>
          </FormControl>

          {/* Status filter */}
          <FormControl size="small" sx={{ minWidth: 130 }}>
            <InputLabel>Trạng thái</InputLabel>
            <Select
              value={selectedStatus}
              label="Trạng thái"
              onChange={(e) => setSelectedStatus(e.target.value)}
            >
              <MenuItem value="">Tất cả</MenuItem>
              <MenuItem value="TODO">To Do</MenuItem>
              <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
              <MenuItem value="REVIEW">Review</MenuItem>
              <MenuItem value="DONE">Done</MenuItem>
            </Select>
          </FormControl>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>{error}</Alert>}

      {/* Semantic Legend */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2, flexWrap: 'wrap' }}>
        <Typography variant="caption" fontWeight={600} sx={{ color: '#84918D' }}>Màu sắc trạng thái:</Typography>
        <Chip size="small" label="Quá hạn" sx={{ bgcolor: '#FCE8E6', color: '#C5221F', fontWeight: 600, border: '1px solid #EA4335' }} />
        <Chip size="small" label="Sắp đến hạn" sx={{ bgcolor: '#FEF7E0', color: '#B06000', fontWeight: 600, border: '1px solid #FBBC04' }} />
        <Chip size="small" label="Đang làm" sx={{ bgcolor: '#E8F0FE', color: '#1967D2', fontWeight: 600, border: '1px solid #4285F4' }} />
        <Chip size="small" label="Đã hoàn thành" sx={{ bgcolor: '#E6F4EA', color: '#137333', fontWeight: 600, border: '1px solid #34A853' }} />
      </Box>

      {/* Calendar Grid */}
      <Card sx={{ borderRadius: 3, border: '1px solid #E2EBE6', boxShadow: 'none' }}>
        {/* Days of week header */}
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', bgcolor: '#F3F6F4', borderBottom: '1px solid #E2EBE6' }}>
          {DAYS_OF_WEEK.map((day) => (
            <Box key={day} sx={{ p: 1.5, textAlign: 'center', fontWeight: 700, fontSize: '0.9375rem', color: '#42504C' }}>
              {day}
            </Box>
          ))}
        </Box>

        {/* Days grid */}
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)' }}>
          {days.map((dateObj) => {
            const dateStr = toDateKey(dateObj);
            const isToday = dateStr === todayStr;
            const isCurrentMonth = dateObj.getMonth() === currentMonth;
            const dayTasks = tasksByDate[dateStr] || [];

            return (
              <Box
                key={dateStr}
                sx={{
                  minHeight: viewMode === 'month' ? 120 : 280,
                  p: 1,
                  borderRight: '1px solid #E2EBE6',
                  borderBottom: '1px solid #E2EBE6',
                  bgcolor: isToday ? 'rgba(15,118,110,0.04)' : isCurrentMonth ? '#fff' : '#FAFCFA',
                  '&:nth-of-type(7n)': { borderRight: 'none' },
                  display: 'flex',
                  flexDirection: 'column',
                }}
              >
                {/* Date number */}
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
                  <Typography
                    variant="caption"
                    fontWeight={isToday ? 800 : 600}
                    sx={{
                      width: 24, height: 24, display: 'flex', alignItems: 'center', justifyContent: 'center',
                      borderRadius: '50%',
                      bgcolor: isToday ? '#0F766E' : 'transparent',
                      color: isToday ? '#fff' : isCurrentMonth ? '#17201F' : '#A3B0AC',
                    }}
                  >
                    {dateObj.getDate()}
                  </Typography>
                  {dayTasks.length > 0 && (
                    <Typography variant="caption" sx={{ color: '#84918D', fontSize: '0.8125rem' }}>
                      {dayTasks.length} task{dayTasks.length > 1 ? 's' : ''}
                    </Typography>
                  )}
                </Box>

                {/* Tasks container */}
                <Box sx={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 0.5, maxHeight: viewMode === 'month' ? 90 : 230 }}>
                  {dayTasks.map((t) => {
                    const colorStyle = getTaskColor(t);
                    return (
                      <Box
                        key={t.id}
                        onClick={() => setSelectedTaskId(t.id)}
                        sx={{
                          p: 0.75,
                          borderRadius: 1.5,
                          bgcolor: colorStyle.bg,
                          borderLeft: `3px solid ${colorStyle.border}`,
                          cursor: 'pointer',
                          transition: 'transform 100ms ease, box-shadow 100ms ease',
                          '&:hover': { transform: 'translateY(-1px)', boxShadow: '0 2px 6px rgba(0,0,0,0.08)' },
                        }}
                      >
                        <Typography
                          variant="caption"
                          fontWeight={600}
                          sx={{
                            color: colorStyle.text,
                            display: '-webkit-box',
                            WebkitLineClamp: 1,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                            fontSize: '0.875rem',
                          }}
                        >
                          {t.title}
                        </Typography>
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mt: 0.25 }}>
                          <Typography variant="caption" sx={{ color: '#84918D', fontSize: '0.75rem' }}>
                            {t.projectName}
                          </Typography>
                          <Typography variant="caption" fontWeight={700} sx={{ color: colorStyle.text, fontSize: '0.75rem' }}>
                            {t.priority}
                          </Typography>
                        </Box>
                      </Box>
                    );
                  })}
                </Box>
              </Box>
            );
          })}
        </Box>
      </Card>

      {/* Task detail modal when clicking a task */}
      {selectedTaskId && (
        <TaskDetailModal
          taskId={selectedTaskId}
          open={Boolean(selectedTaskId)}
          onClose={() => {
            setSelectedTaskId(null);
            fetchTasks();
          }}
          onUpdate={() => fetchTasks()}
          onDelete={() => {
            setSelectedTaskId(null);
            fetchTasks();
          }}
        />
      )}
    </Box>
  );
}
