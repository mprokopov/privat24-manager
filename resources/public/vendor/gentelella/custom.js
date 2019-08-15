$('#testDate').daterangepicker({
    ranges: {
        // 'Сегодня': [moment(), moment()],
        // 'Вчера': [moment().subtract(1, 'days'), moment().subtract(1, 'days')],
        // 'Последние 7 дней': [moment().subtract(6, 'days'), moment()],
        // 'Последние 30 дней': [moment().subtract(29, 'days'), moment()], 
        'Этот месяц': [moment().startOf('month'), moment().endOf('month')],
        'Прошлый месяц': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
        'Позапрошлый месяц': [moment().subtract(2, 'month').startOf('month'), moment().subtract(2, 'month').endOf('month')]
    },
    locale: {
        firstDay: 1,
        "autoApply": true,
        format: 'DD-MM-YYYY',
        "applyLabel": "Применить",
        "customRangeLabel": "Выбрать",
        "cancelLabel": "Отмена",
        "daysOfWeek": [
            "Вс",
            "Пн",
            "Вт",
            "Ср",
            "Чт",
            "Пт",
            "Сб"
        ],
        "monthNames": [
            "Январь",
            "February",
            "Март",
            "Апрель",
            "Май",
            "Июнь",
            "Июль",
            "Август",
            "Сентябрь",
            "Октябрь",
            "Ноябрь",
            "Декабрь"
        ],

    },
    "alwaysShowCalendars": true,
    "maxDate": moment(),
}, function (start, end, label) {
    $('input[name="stdate"]').val(start.format('DD-MM-YYYY'));
    $('input[name="endate"]').val(end.format('DD-MM-YYYY'));
});
