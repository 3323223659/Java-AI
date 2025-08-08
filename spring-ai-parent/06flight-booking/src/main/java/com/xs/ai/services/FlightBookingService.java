package com.xs.ai.services;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.xs.ai.data.*;
import org.springframework.stereotype.Service; 

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class FlightBookingService {

	private final BookingData db;

	public FlightBookingService() {
		db = new BookingData();
		initDemoData();
	}

	private void initDemoData() {
		List<String> names = List.of("徐庶", "诸葛", "百里", "楼兰", "庄周");
		List<String> airportCodes = List.of("北京", "上海", "广州", "深圳", "杭州", "南京", "青岛", "成都", "武汉", "西安", "重庆", "大连",
				"天津");
		Random random = new Random();

		var customers = new ArrayList<Customer>();
		var bookings = new ArrayList<Booking>();

		for (int i = 0; i < 5; i++) {
			String name = names.get(i);
			String from = airportCodes.get(random.nextInt(airportCodes.size()));
			String to = airportCodes.get(random.nextInt(airportCodes.size()));
			BookingClass bookingClass = BookingClass.values()[random.nextInt(BookingClass.values().length)];
			Customer customer = new Customer();
			customer.setName(name);

			LocalDate date = LocalDate.now().plusDays(2 * (i + 1));

			Booking booking = new Booking("10" + (i + 1), date, customer, BookingStatus.CONFIRMED, from, to,
					bookingClass);
			customer.getBookings().add(booking);

			customers.add(customer);
			bookings.add(booking);
		}

		// Reset the database on each start
		db.setCustomers(customers);
		db.setBookings(bookings);
	}


	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record BookingDetails(String bookingNumber, String name, LocalDate begindate, BookingStatus bookingStatus,
								 String from, String to, String bookingClass) {
	}
	// 获取所有已预订航班
	public List<BookingDetails> getBookings() {
		return db.getBookings().stream().map(this::toBookingDetails).toList();
	}

	// 根据编号+姓名查询航班
	private Booking findBooking(String bookingNumber, String name) {
		return db.getBookings()
			.stream()
			.filter(b -> b.getBookingNumber().equalsIgnoreCase(bookingNumber))
			.filter(b -> b.getCustomer().getName().equalsIgnoreCase(name))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Booking not found"));
	}

	// 查询航班详情
	public BookingDetails getBookingDetails(String bookingNumber, String name) {
		var booking = findBooking(bookingNumber, name);

		// 数据库优化
		return toBookingDetails(booking);
	}

	// 更改预定航班
	public void changeBooking(String bookingNumber, String name, String newDate, String from, String to) {
		var booking = findBooking(bookingNumber, name);
		if (booking.getDate().isBefore(LocalDate.now().plusDays(1))) {
			throw new IllegalArgumentException("Booking cannot be changed within 24 hours of the start date.");
		}
		booking.setDate(LocalDate.parse(newDate));
		booking.setFrom(from);
		booking.setTo(to);
	}

	// 取消预定航班
	public void cancelBooking(String bookingNumber, String name) {
		var booking = findBooking(bookingNumber, name);
		if (booking.getDate().isBefore(LocalDate.now().plusDays(2))) {
			throw new IllegalArgumentException("Booking cannot be cancelled within 48 hours of the start date.");
		}


		booking.setBookingStatus(BookingStatus.CANCELLED);
	}

	private BookingDetails toBookingDetails(Booking booking) {
		return new BookingDetails(booking.getBookingNumber(), booking.getCustomer().getName(), booking.getDate(),
				booking.getBookingStatus(), booking.getFrom(), booking.getTo(), booking.getBookingClass().toString());
	}

}
