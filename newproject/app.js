// Web Todo App Controller
(function() {
    const STORAGE_KEY = 'todo_projects';
    let projects = [];

    // DOM Elements
    const cardsContainer = document.getElementById('cards-container');
    const projectInput = document.getElementById('project-input');
    const projectEmoji = document.getElementById('project-emoji');
    const addProjectBtn = document.getElementById('add-project-btn');
    const statsLabel = document.getElementById('stats-label');
    const filterCombo = document.getElementById('filter-combo');
    const searchInput = document.getElementById('search-input');

    // Date formatting helper (converts YYYY-MM-DD to e.g. Jun 25, 2026)
    function formatDate(dateStr) {
        if (!dateStr) return '';
        const parts = dateStr.split('-');
        if (parts.length === 3) {
            const year = parts[0];
            const monthIdx = parseInt(parts[1]) - 1;
            const day = parseInt(parts[2]);
            const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            if (monthIdx >= 0 && monthIdx < 12) {
                return `${months[monthIdx]} ${day}, ${year}`;
            }
        }
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) return dateStr;
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        return `${months[date.getMonth()]} ${date.getDate()}, ${date.getFullYear()}`;
    }

    // Overdue validator (relative to current date at midnight)
    function isOverdue(dueDateStr) {
        if (!dueDateStr) return false;
        const dueDate = new Date(dueDateStr);
        if (isNaN(dueDate.getTime())) return false;
        
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        dueDate.setHours(0, 0, 0, 0);
        
        return dueDate < today;
    }

    // Seed sample data if localStorage is empty (customized to match user mockup)
    function loadData() {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
            try {
                projects = JSON.parse(stored);
                // Backward compatibility data migration: ensure tasks have priority and date fields
                projects.forEach(p => {
                    p.tasks.forEach(t => {
                        if (!t.priority) t.priority = 'low';
                        if (!t.dueDate) {
                            const todayStr = new Date().toISOString().split('T')[0];
                            t.dueDate = todayStr;
                        }
                    });
                });
            } catch (e) {
                console.error("Failed to parse stored projects, creating fresh list.", e);
                projects = [];
            }
        } else {
            // Seed sample data representing the mockup state
            const todayStr = new Date().toISOString().split('T')[0];
            
            // To make "Book flight", "Pack bags", "Buy souvenirs" match the mockup dates:
            // "Jun 25, 2026", "Jun 28, 2026", "Jun 30, 2026" respectively.
            projects = [
                {
                    name: "Trip",
                    emoji: "🏕️",
                    tasks: [
                        { name: "Book flight", completed: true, priority: "high", dueDate: "2026-06-25" },
                        { name: "Pack bags", completed: false, priority: "medium", dueDate: "2026-06-28" },
                        { name: "Buy souvenirs", completed: false, priority: "low", dueDate: "2026-06-30" }
                    ]
                }
            ];
            saveData();
        }
    }

    function saveData() {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(projects));
    }

    // Calculate and update global header statistics
    function updateGlobalStats() {
        const projectCount = projects.length;
        let totalTasks = 0;
        let completedTasks = 0;
        let overdueTasks = 0;

        projects.forEach(p => {
            p.tasks.forEach(t => {
                totalTasks++;
                if (t.completed) {
                    completedTasks++;
                } else if (isOverdue(t.dueDate)) {
                    overdueTasks++;
                }
            });
        });

        const donePercent = totalTasks === 0 ? 0 : Math.round((completedTasks * 100) / totalTasks);

        const projText = projectCount + " project" + (projectCount !== 1 ? "s" : "");
        const taskText = totalTasks + " task" + (totalTasks !== 1 ? "s" : "");
        statsLabel.textContent = `${projText} · ${taskText}`;

        document.getElementById('global-done-percent').textContent = `${donePercent}%`;
        document.getElementById('global-overdue-count').textContent = overdueTasks;
        document.getElementById('global-tasks-ratio').textContent = `${completedTasks}/${totalTasks}`;
    }

    // Create a DOM Element helper
    function createElement(tag, className, htmlContent = '') {
        const el = document.createElement(tag);
        if (className) el.className = className;
        if (htmlContent) el.innerHTML = htmlContent;
        return el;
    }

    // Render all projects
    function init() {
        loadData();
        renderAllProjectCards();
        updateGlobalStats();

        // Global Event Listeners
        addProjectBtn.addEventListener('click', handleAddProject);
        projectInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') handleAddProject();
        });

        filterCombo.addEventListener('change', () => {
            document.querySelectorAll('.project-card').forEach(cardEl => {
                const projectIndex = cardEl.dataset.index;
                const project = projects[projectIndex];
                renderTasksForCard(cardEl, project);
            });
        });

        // Search Bar Key/Input listener
        searchInput.addEventListener('input', () => {
            const query = searchInput.value.toLowerCase().trim();
            document.querySelectorAll('.project-card').forEach(cardEl => {
                const projectIndex = cardEl.dataset.index;
                const project = projects[projectIndex];
                
                const titleStr = `${project.emoji || ''} ${project.name}`.toLowerCase();
                if (titleStr.includes(query)) {
                    cardEl.style.display = '';
                } else {
                    cardEl.style.display = 'none';
                }
            });
        });
    }

    function handleAddProject() {
        const name = projectInput.value.trim();
        const emoji = projectEmoji.value;
        if (!name) return;

        const newProject = {
            name: name,
            emoji: emoji,
            tasks: []
        };

        projects.push(newProject);
        saveData();

        renderAllProjectCards();
        updateGlobalStats();

        // Reset inputs
        projectInput.value = '';
        projectEmoji.selectedIndex = 0;
    }

    function renderAllProjectCards() {
        cardsContainer.innerHTML = '';
        projects.forEach((project, index) => {
            const cardEl = createProjectCardElement(project, index);
            cardsContainer.appendChild(cardEl);
        });
    }

    // Create Project Card Element
    function createProjectCardElement(project, index) {
        const card = createElement('div', 'project-card');
        card.dataset.index = index;

        // Card Header
        const cardHeader = createElement('div', 'card-header');
        
        const titleSection = createElement('div', 'card-title-section');
        const title = createElement('div', 'card-title', `${project.emoji || '🚀'} ${project.name}`);
        
        const statsRow = createElement('div', 'card-stats');
        const counter = createElement('span', 'card-counter', '0/0 tasks');
        const percent = createElement('span', 'card-percent', '0%');
        statsRow.appendChild(counter);
        statsRow.appendChild(percent);
        
        titleSection.appendChild(title);
        titleSection.appendChild(statsRow);

        const actions = createElement('div', 'card-actions');
        const toggleBtn = createElement('button', 'btn-icon btn-toggle', '▼');
        const deleteBtn = createElement('button', 'btn-icon btn-delete-card', '×');
        actions.appendChild(toggleBtn);
        actions.appendChild(deleteBtn);

        cardHeader.appendChild(titleSection);
        cardHeader.appendChild(actions);
        card.appendChild(cardHeader);

        // Progress Bar
        const progressBarContainer = createElement('div', 'progress-bar-container');
        const progressBarFill = createElement('div', 'progress-bar-fill');
        progressBarContainer.appendChild(progressBarFill);
        card.appendChild(progressBarContainer);

        // Collapsible container for tasks
        const collapsibleContainer = createElement('div', 'collapsible-container');
        
        // Tasks wrapper (holds list of tasks)
        const tasksWrapper = createElement('div', 'tasks-wrapper');
        collapsibleContainer.appendChild(tasksWrapper);

        // Inline Add Task panel
        const addTaskPanel = createElement('div', 'add-task-panel');
        
        // Dynamic form creator
        const expandAddBtn = createElement('button', 'btn-expand-add-task', '+ Add Task');
        addTaskPanel.appendChild(expandAddBtn);
        collapsibleContainer.appendChild(addTaskPanel);

        card.appendChild(collapsibleContainer);

        // Render tasks & calculate metrics
        renderTasksForCard(card, project);
        updateCardProgress(card, project);

        // Toggle Expand/Collapse
        toggleBtn.addEventListener('click', () => {
            const isExpanded = collapsibleContainer.classList.toggle('expanded');
            toggleBtn.textContent = isExpanded ? '▲' : '▼';
        });

        // Delete Project Card
        deleteBtn.addEventListener('click', () => {
            card.classList.add('removing');
            card.addEventListener('animationend', () => {
                const cardIndex = parseInt(card.dataset.index);
                projects.splice(cardIndex, 1);
                saveData();
                renderAllProjectCards();
                updateGlobalStats();
            }, { once: true });
        });

        // Expand Add Task panel to show input form
        expandAddBtn.addEventListener('click', () => {
            expandAddBtn.style.display = 'none';

            const form = createElement('div', 'add-task-form');
            
            const rowMain = createElement('div', 'form-row-main');
            const taskInput = createElement('input', 'input-modern');
            taskInput.type = 'text';
            taskInput.placeholder = 'Add a new task...';
            taskInput.autocomplete = 'off';
            rowMain.appendChild(taskInput);

            const rowMeta = createElement('div', 'form-row-meta');
            
            const prioritySelect = createElement('select', 'form-select');
            prioritySelect.innerHTML = `
                <option value="high">🔴 HIGH</option>
                <option value="medium" selected>🟡 MED</option>
                <option value="low">🟢 LOW</option>
            `;

            const dateInput = createElement('input', 'form-date');
            dateInput.type = 'date';
            dateInput.value = new Date().toISOString().split('T')[0]; // Default to today

            const formButtons = createElement('div', 'form-buttons');
            const cancelBtn = createElement('button', 'btn-secondary', 'Cancel');
            const submitBtn = createElement('button', 'btn-accent btn-add-task-submit', 'Add');

            formButtons.appendChild(cancelBtn);
            formButtons.appendChild(submitBtn);

            rowMeta.appendChild(prioritySelect);
            rowMeta.appendChild(dateInput);
            rowMeta.appendChild(formButtons);

            form.appendChild(rowMain);
            form.appendChild(rowMeta);
            addTaskPanel.appendChild(form);

            taskInput.focus();

            // Submit Handler
            const submitTask = () => {
                const desc = taskInput.value.trim();
                if (!desc) return;

                const newTask = {
                    name: desc,
                    completed: false,
                    priority: prioritySelect.value,
                    dueDate: dateInput.value
                };

                project.tasks.push(newTask);
                saveData();

                // Reset add task UI back to expand button
                form.remove();
                expandAddBtn.style.display = '';

                // Refresh UI
                renderTasksForCard(card, project);
                updateCardProgress(card, project);
                updateGlobalStats();
            };

            submitBtn.addEventListener('click', submitTask);
            taskInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') submitTask();
            });

            cancelBtn.addEventListener('click', () => {
                form.remove();
                expandAddBtn.style.display = '';
            });
        });

        return card;
    }

    // Render task items & display completed celebration banner if necessary
    function renderTasksForCard(cardEl, project) {
        const tasksWrapper = cardEl.querySelector('.tasks-wrapper');
        const collapsibleContainer = cardEl.querySelector('.collapsible-container');
        tasksWrapper.innerHTML = '';
        
        const filter = filterCombo.value;

        project.tasks.forEach((task, taskIndex) => {
            const matches = 
                (filter === 'All') ||
                (filter === 'Active' && !task.completed) ||
                (filter === 'Completed' && task.completed);

            if (!matches) return;

            const row = createElement('div', 'task-row');
            
            // Left Section (Checkbox & Text)
            const leftSection = createElement('div', 'task-left-section');
            const label = createElement('label', 'checkbox-label');
            const checkbox = createElement('input', 'checkbox-input');
            checkbox.type = 'checkbox';
            checkbox.checked = task.completed;
            const customBox = createElement('span', 'checkbox-custom');
            const labelText = createElement('span', 'task-text', task.name);
            label.appendChild(checkbox);
            label.appendChild(customBox);
            label.appendChild(labelText);
            leftSection.appendChild(label);
            row.appendChild(leftSection);

            // Right Section (Priority badge, Due Date, Delete button)
            const metaSection = createElement('div', 'task-meta-section');
            
            // Priority Tag
            const pBadge = createElement('span', `priority-badge priority-${task.priority}`);
            let pEmoji = '🟢';
            if (task.priority === 'high') pEmoji = '🔴';
            if (task.priority === 'medium') pEmoji = '🟡';
            pBadge.innerHTML = `${pEmoji} ${task.priority.toUpperCase()}`;
            metaSection.appendChild(pBadge);

            // Due Date Tag
            const dTag = createElement('span', 'task-due-date');
            if (isOverdue(task.dueDate) && !task.completed) {
                dTag.classList.add('overdue');
            }
            dTag.innerHTML = `📅 ${formatDate(task.dueDate)}`;
            metaSection.appendChild(dTag);

            // Delete Button
            const delTaskBtn = createElement('button', 'btn-icon btn-delete-task', '×');
            metaSection.appendChild(delTaskBtn);

            row.appendChild(metaSection);
            tasksWrapper.appendChild(row);

            // Checkbox event listener
            checkbox.addEventListener('change', () => {
                task.completed = checkbox.checked;
                saveData();
                updateCardProgress(cardEl, project);
                updateGlobalStats();
                // Check celebration state immediately
                checkCelebrationState(cardEl, project);
            });

            // Delete task event listener
            delTaskBtn.addEventListener('click', () => {
                row.classList.add('removing');
                row.addEventListener('animationend', () => {
                    project.tasks.splice(taskIndex, 1);
                    saveData();
                    renderTasksForCard(cardEl, project);
                    updateCardProgress(cardEl, project);
                    updateGlobalStats();
                }, { once: true });
            });
        });

        // Trigger celebration card update
        checkCelebrationState(cardEl, project);
    }

    // Celebration checking logic
    function checkCelebrationState(cardEl, project) {
        const collapsibleContainer = cardEl.querySelector('.collapsible-container');
        const existingBanner = cardEl.querySelector('.celebration-banner');
        if (existingBanner) existingBanner.remove();

        const total = project.tasks.length;
        let completed = 0;
        project.tasks.forEach(t => {
            if (t.completed) completed++;
        });

        if (total > 0 && completed === total) {
            const banner = createElement('div', 'celebration-banner', `🎉 All tasks completed in ${project.name}!`);
            const addTaskPanel = cardEl.querySelector('.add-task-panel');
            collapsibleContainer.insertBefore(banner, addTaskPanel);
        }
    }

    // Update Progress Indicators for a Project Card
    function updateCardProgress(cardEl, project) {
        const total = project.tasks.length;
        let completed = 0;
        project.tasks.forEach(t => {
            if (t.completed) completed++;
        });

        const percentVal = total === 0 ? 0 : Math.round((completed * 100) / total);

        const counterLabel = cardEl.querySelector('.card-counter');
        const percentLabel = cardEl.querySelector('.card-percent');
        const progressBarFill = cardEl.querySelector('.progress-bar-fill');

        counterLabel.textContent = `${completed}/${total} task${total !== 1 ? 's' : ''}`;
        percentLabel.textContent = `${percentVal}%`;
        progressBarFill.style.width = `${percentVal}%`;
    }

    // Start App
    window.addEventListener('DOMContentLoaded', init);
})();
