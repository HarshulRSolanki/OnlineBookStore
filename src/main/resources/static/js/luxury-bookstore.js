/**
 * Quickread - Luxury Bookstore Interactive Features & Enhancements
 */

document.addEventListener('DOMContentLoaded', function () {
    // 1. Sticky Navigation on Scroll
    const navbar = document.querySelector('.luxury-navbar');
    if (navbar) {
        window.addEventListener('scroll', function () {
            if (window.scrollY > 25) {
                navbar.classList.add('scrolled');
            } else {
                navbar.classList.remove('scrolled');
            }
        });
    }

    // 2. Password Visibility Toggles
    const toggleBtns = document.querySelectorAll('.password-toggle-btn');
    toggleBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            const input = this.closest('.auth-input-wrapper')?.querySelector('input') ||
                          this.previousElementSibling;
            if (input) {
                const isPassword = input.getAttribute('type') === 'password';
                input.setAttribute('type', isPassword ? 'text' : 'password');
                const icon = this.querySelector('i');
                if (icon) {
                    icon.classList.toggle('fa-eye', !isPassword);
                    icon.classList.toggle('fa-eye-slash', isPassword);
                }
            }
        });
    });

    // 3. Smooth Anchor Scrolling
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const targetId = this.getAttribute('href');
            if (targetId && targetId !== '#') {
                const target = document.querySelector(targetId);
                if (target) {
                    e.preventDefault();
                    target.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            }
        });
    });

    // 4. Client-side Category & Search Filter Enhancements
    const categoryPills = document.querySelectorAll('.category-filter-pill');
    const bookCards = document.querySelectorAll('.book-card-item');

    categoryPills.forEach(pill => {
        pill.addEventListener('click', function () {
            const categoryName = this.getAttribute('data-category');
            categoryPills.forEach(p => p.classList.remove('active'));
            this.classList.add('active');

            if (!categoryName || categoryName === 'all') {
                bookCards.forEach(card => card.style.display = '');
            } else {
                bookCards.forEach(card => {
                    const cardCategory = card.getAttribute('data-category');
                    if (cardCategory && cardCategory.toLowerCase() === categoryName.toLowerCase()) {
                        card.style.display = '';
                    } else {
                        card.style.display = 'none';
                    }
                });
            }
        });
    });

    // 5. Toast Notification Helper
    window.showLuxuryToast = function (message, type = 'info') {
        let toast = document.getElementById('luxury-toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'luxury-toast';
            toast.className = 'editorial-toast';
            toast.innerHTML = `<i class="fas fa-check-circle"></i> <span class="toast-msg"></span>`;
            document.body.appendChild(toast);
        }
        const msgSpan = toast.querySelector('.toast-msg');
        if (msgSpan) msgSpan.textContent = message;
        toast.classList.add('show');
        setTimeout(() => {
            toast.classList.remove('show');
        }, 3500);
    };

    // 6. Quantity Controls (if present)
    const qtyInputs = document.querySelectorAll('.quantity-input');
    qtyInputs.forEach(input => {
        const minus = input.parentElement.querySelector('.qty-minus');
        const plus = input.parentElement.querySelector('.qty-plus');
        if (minus && plus) {
            minus.addEventListener('click', () => {
                let val = parseInt(input.value) || 1;
                if (val > 1) input.value = val - 1;
            });
            plus.addEventListener('click', () => {
                let val = parseInt(input.value) || 1;
                input.value = val + 1;
            });
        }
    });

    // 7. Publish Date Formatter: converts HTML5 YYYY-MM-DD to DD-MM-YYYY on submit for Spring @DateTimeFormat compatibility
    document.addEventListener('submit', function (e) {
        const form = e.target;
        if (!form) return;
        const dateInput = form.querySelector('input[name="publishDate"]');
        if (dateInput && dateInput.value) {
            const val = dateInput.value.trim();
            const ymdMatch = val.match(/^(\d{4})-(\d{2})-(\d{2})$/);
            if (ymdMatch) {
                dateInput.type = 'text';
                dateInput.value = `${ymdMatch[3]}-${ymdMatch[2]}-${ymdMatch[1]}`;
            }
        }
    });
});

